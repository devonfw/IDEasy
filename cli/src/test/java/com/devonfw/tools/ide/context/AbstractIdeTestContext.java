package com.devonfw.tools.ide.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.commandlet.TestCommandletManager;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesSystem;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.IdeSystem;
import com.devonfw.tools.ide.environment.IdeSystemTestImpl;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarTestImpl;
import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.network.NetworkStatusMock;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.os.WindowsHelperMock;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class AbstractIdeTestContext extends AbstractIdeContext {

  /** {@link Path} to use as workingDirectory for mocking. */
  public static final Path PATH_MOCK = Path.of("/");

  private String[] answers;

  private int answerIndex;

  private final Map<String, IdeProgressBarTestImpl> progressBarMap;

  private SystemInfo systemInfo;

  private ProcessContext mockContext;

  private TestCommandletManager testCommandletManager;

  private Path ideRoot;

  private boolean ideRootSet;

  private Path urlsPath;

  protected final WireMockRuntimeInfo wireMockRuntimeInfo;

  private NetworkStatusMock networkStatus;

  /**
   * The constructor.
   *
   * @param logger the {@link IdeLogger}.
   * @param workingDirectory the optional {@link Path} to current working directory.
   * @param wireMockRuntimeInfo wireMock server on a random port.
   */
  public AbstractIdeTestContext(IdeStartContextImpl logger, Path workingDirectory, WireMockRuntimeInfo wireMockRuntimeInfo) {

    super(logger, workingDirectory);
    this.answers = new String[0];
    this.progressBarMap = new HashMap<>();
    this.systemInfo = super.getSystemInfo();
    this.wireMockRuntimeInfo = wireMockRuntimeInfo;
  }

  /**
   * Finds the test project root by looking for common test project markers. Searches upward from the working directory to find the boundary of the test
   * project.
   *
   * @param workingDirectory the starting directory.
   * @return the test project root or {@code null} if not found.
   */
  private Path findTestProjectRoot(Path workingDirectory) {

    if (workingDirectory == null || workingDirectory.equals(PATH_MOCK)) {
      return null;
    }

    Path current = workingDirectory.toAbsolutePath();
    Path testResourcesMarker = Path.of("src", "test", "resources", "ide-projects");

    // Look for the parent of ide-projects test resources
    while (current != null) {
      Path ideProjectsPath = current.resolve(testResourcesMarker);
      if (Files.isDirectory(ideProjectsPath)) {
        // Found the project root containing test resources
        return current;
      }
      current = current.getParent();
    }

    // If we can't find test resources, use the working directory's parent as boundary
    // This provides at least some protection
    return workingDirectory.getParent();
  }

  @Override
  protected IdeHomeAndWorkspace findIdeHome(Path workingDirectory) {

    // Set test boundary to prevent IDE home detection from escaping test projects
    Path testBoundary = findTestProjectRoot(workingDirectory);

    // Call parent implementation to perform the search
    IdeHomeAndWorkspace result = super.findIdeHome(workingDirectory);

    // Validate that the detected IDE home (if any) is within test boundaries
    Path ideHome = result.home();
    if (ideHome != null && testBoundary != null && !ideHome.startsWith(testBoundary)) {
      debug("Test isolation violation: Detected IDE home '{}' is outside test boundary '{}'.\n"
          + "This indicates the test project structure is incomplete or improperly configured.\n"
          + "A valid IDE home directories is determined by isIdeHome() method.\n"
          + "Please ensure your test project has the required structure.", ideHome, testBoundary);
      return null;
    }
    return result;
  }

  @Override
  public boolean isTest() {

    return true;
  }

  /**
   * @return {@code true} if mutable, {@code false} otherwise.
   * @see IdeTestContextMock
   */
  protected boolean isMutable() {

    return true;
  }

  private void requireMutable() {

    if (!isMutable()) {
      throw new IllegalStateException(getClass().getSimpleName() + " is immutable!");
    }
  }

  @Override
  protected String readLine() {

    if (this.answerIndex >= this.answers.length) {
      throw new IllegalStateException("End of answers reached!");
    }
    String answer = this.answers[this.answerIndex++];
    interaction(answer);
    return answer;
  }

  /**
   * @param answers the answers for interactive questions in order (e.g. if "yes" is given as first answer, this will be used to answer the first
   *     question).
   */
  public void setAnswers(String... answers) {
    requireMutable();
    this.answers = answers;
    this.answerIndex = 0;
  }

  /**
   * @return Map of progress bars with task name and actual implementation.
   */
  public Map<String, IdeProgressBarTestImpl> getProgressBarMap() {

    return this.progressBarMap;
  }

  @Override
  public IdeProgressBar newProgressBar(String title, long maxSize, String unitName, long unitSize) {

    IdeProgressBarTestImpl progressBar = new IdeProgressBarTestImpl(title, maxSize, unitName, unitSize);
    IdeProgressBarTestImpl duplicate = this.progressBarMap.put(title, progressBar);
    // If we have multiple downloads or unpacking, we may have an existing "Downloading" or "Unpacking" key
    assert (title.equals(IdeProgressBar.TITLE_DOWNLOADING)) || (title.equals(IdeProgressBar.TITLE_EXTRACTING)) || (title.equals(
        IdeProgressBar.TITLE_INSTALL_PLUGIN)) || duplicate == null;
    return progressBar;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected AbstractEnvironmentVariables createSystemVariables() {

    Path home = getUserHome();
    if (home != null) {
      Path environmentPropertiesFile = home.resolve("environment.properties");
      if (Files.exists(environmentPropertiesFile)) {
        Properties environmentProperties = getFileAccess().readProperties(environmentPropertiesFile);
        if (SystemInfoImpl.INSTANCE.isWindows()) {
          String path = environmentProperties.getProperty(IdeVariables.PATH.getName());
          if (path != null) {
            path = path.replace(':', ';');
            environmentProperties.setProperty(IdeVariables.PATH.getName(), path);
          }
        }
        return EnvironmentVariablesSystem.of(this, (Map) environmentProperties);
      }
    }
    return super.createSystemVariables();
  }

  @Override
  public IdeSystemTestImpl getSystem() {

    if (this.system == null) {
      this.system = new IdeSystemTestImpl(this);
    }
    return (IdeSystemTestImpl) this.system;
  }

  /**
   * @param system the new value of {@link #getSystem()}.
   */
  public void setSystem(IdeSystem system) {

    this.system = system;
  }

  @Override
  public WindowsHelper createWindowsHelper() {

    return new WindowsHelperMock(this);
  }

  @Override
  protected SystemPath computeSystemPath() {

    EnvironmentVariables systemVars = getVariables().getByType(EnvironmentVariablesType.SYSTEM);
    String envPath = systemVars.get(IdeVariables.PATH.getName());
    envPath = getVariables().resolve(envPath, systemVars.getSource());
    return new SystemPath(this, envPath);
  }

  @Override
  public NetworkStatusMock getNetworkStatus() {

    if (this.networkStatus == null) {
      this.networkStatus = new NetworkStatusMock(this, this.wireMockRuntimeInfo);
    }
    return this.networkStatus;
  }

  @Override
  public ProcessContext newProcess() {

    if (this.mockContext != null) {
      return this.mockContext;
    }
    return super.newProcess();
  }

  /**
   * @param mockContext the instance to mock {@link #newProcess()}.
   */
  public void setProcessContext(ProcessContext mockContext) {

    requireMutable();
    this.mockContext = mockContext;
  }

  @Override
  public SystemInfo getSystemInfo() {

    return this.systemInfo;
  }

  /**
   * @param systemInfo the {@link SystemInfo} to use for testing.
   * @see com.devonfw.tools.ide.os.SystemInfoMock
   */
  public void setSystemInfo(SystemInfo systemInfo) {

    requireMutable();
    this.systemInfo = systemInfo;
  }

  @Override
  public void setUserHome(Path dummyUserHome) {

    requireMutable();
    super.setUserHome(dummyUserHome);
  }

  @Override
  public Path getIdeRoot() {

    if (this.ideRootSet) {
      return this.ideRoot;
    }
    return super.getIdeRoot();
  }

  /**
   * @param ideRoot the new value of {@link #getIdeRoot()}.
   */
  public void setIdeRoot(Path ideRoot) {

    this.ideRoot = ideRoot;
    this.ideRootSet = true;
  }

  @Override
  public Path getUrlsPath() {

    if (this.urlsPath == null) {
      return super.getUrlsPath();
    }
    return this.urlsPath;
  }

  /**
   * @param urlsPath the mocked {@link #getUrlsPath() urls path}.
   */
  public void setUrlsPath(Path urlsPath) {

    this.urlsPath = urlsPath;
    this.urlMetadata = new UrlMetadata(this);
  }

  /**
   * @param defaultToolRepository the new value of {@link #getDefaultToolRepository()}.
   */
  public void setDefaultToolRepository(ToolRepository defaultToolRepository) {

    this.defaultToolRepository = defaultToolRepository;
  }

  /**
   * @param settingsPath the new value of {@link #getSettingsPath()}.
   */
  public void setSettingsPath(Path settingsPath) {

    this.settingsPath = settingsPath;
  }

  /**
   * @param pluginsPath the new value of {@link #getPluginsPath()}.
   */
  public void setPluginsPath(Path pluginsPath) {

    this.pluginsPath = pluginsPath;
  }

  /**
   * @param commandletManager the new value of {@link #getCommandletManager()}.
   */
  public void setCommandletManager(CommandletManager commandletManager) {
    if (commandletManager instanceof TestCommandletManager tcm) {
      this.testCommandletManager = tcm;
    } else {
      this.testCommandletManager = null;
    }
    this.commandletManager = commandletManager;
  }

  /**
   * @param commandlet the {@link Commandlet} to add to {@link #getCommandletManager()} for testing.
   */
  public void addCommandlet(Commandlet commandlet) {

    if (this.testCommandletManager == null) {
      setCommandletManager(new TestCommandletManager(this));
    }
    this.testCommandletManager.add(commandlet);
  }

  @Override
  protected Path getIdeRootPathFromEnv(boolean withSanityCheck) {

    Path workingDirectory = getCwd();
    Path root = Path.of("").toAbsolutePath();
    if (root.getRoot().equals(workingDirectory.getRoot())) {
      Path relative = root.relativize(workingDirectory);
      int nameCount = relative.getNameCount();
      if ((nameCount >= 4) && relative.getName(0).toString().contains("target") && relative.getName(1).toString().equals("test-projects")) {
        int rest = nameCount - 2;
        Path ideRoot = workingDirectory;
        while (rest > 0) {
          ideRoot = ideRoot.getParent();
          rest--;
        }
        return ideRoot;
      }
    }
    return null;
  }

  @Override
  public String getDefaultWindowsGitPath() {
    return "";
  }

  @Override
  protected Path findBashInWindowsRegistry() {
    return null;
  }
}
