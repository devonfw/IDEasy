package com.devonfw.tools.ide.context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.commandlet.CommandletManagerImpl;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.commandlet.EnvironmentCommandlet;
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.IdeSystem;
import com.devonfw.tools.ide.environment.IdeSystemImpl;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitContextImpl;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.merge.DirectoryMerger;
import com.devonfw.tools.ide.migration.IdeMigrator;
import com.devonfw.tools.ide.network.NetworkProxy;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.os.WindowsHelperImpl;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.step.StepImpl;
import com.devonfw.tools.ide.tool.repository.CustomToolRepository;
import com.devonfw.tools.ide.tool.repository.CustomToolRepositoryImpl;
import com.devonfw.tools.ide.tool.repository.DefaultToolRepository;
import com.devonfw.tools.ide.tool.repository.MavenRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.util.DateTimeUtil;
import com.devonfw.tools.ide.validation.ValidationResult;
import com.devonfw.tools.ide.validation.ValidationResultValid;
import com.devonfw.tools.ide.validation.ValidationState;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Abstract base implementation of {@link IdeContext}.
 */
public abstract class AbstractIdeContext implements IdeContext {

  private static final GitUrl IDE_URLS_GIT = new GitUrl("https://github.com/devonfw/ide-urls.git", null);

  private static final String LICENSE_URL = "https://github.com/devonfw/IDEasy/blob/main/documentation/LICENSE.adoc";

  private final IdeStartContextImpl startContext;

  private Path ideHome;

  private final Path ideRoot;

  private Path confPath;

  protected Path settingsPath;

  private Path settingsCommitIdPath;

  protected Path pluginsPath;

  private Path workspacePath;

  private String workspaceName;

  private Path cwd;

  private Path downloadPath;

  protected Path userHome;

  private Path userHomeIde;

  private SystemPath path;

  private WindowsPathSyntax pathSyntax;

  private final SystemInfo systemInfo;

  private EnvironmentVariables variables;

  private final FileAccess fileAccess;

  protected CommandletManager commandletManager;

  protected ToolRepository defaultToolRepository;

  private CustomToolRepository customToolRepository;

  private MavenRepository mavenRepository;

  private DirectoryMerger workspaceMerger;

  protected UrlMetadata urlMetadata;

  protected Path defaultExecutionDirectory;

  private StepImpl currentStep;

  protected Boolean online;

  protected IdeSystem system;

  private NetworkProxy networkProxy;

  private WindowsHelper windowsHelper;

  /**
   * The constructor.
   *
   * @param startContext the {@link IdeLogger}.
   * @param workingDirectory the optional {@link Path} to current working directory.
   */
  public AbstractIdeContext(IdeStartContextImpl startContext, Path workingDirectory) {

    super();
    this.startContext = startContext;
    this.systemInfo = SystemInfoImpl.INSTANCE;
    this.commandletManager = new CommandletManagerImpl(this);
    this.fileAccess = new FileAccessImpl(this);
    String workspace = WORKSPACE_MAIN;
    if (workingDirectory == null) {
      workingDirectory = Path.of(System.getProperty("user.dir"));
    } else {
      workingDirectory = workingDirectory.toAbsolutePath();
    }
    this.cwd = workingDirectory;
    // detect IDE_HOME and WORKSPACE
    Path currentDir = workingDirectory;
    String name1 = "";
    String name2 = "";
    Path ideRootPath = getIdeRootPathFromEnv();
    while (currentDir != null) {
      trace("Looking for IDE_HOME in {}", currentDir);
      if (isIdeHome(currentDir)) {
        if (FOLDER_WORKSPACES.equals(name1) && !name2.isEmpty()) {
          workspace = name2;
        }
        break;
      }
      name2 = name1;
      int nameCount = currentDir.getNameCount();
      if (nameCount >= 1) {
        name1 = currentDir.getName(nameCount - 1).toString();
      }
      currentDir = currentDir.getParent();
      if ((ideRootPath != null) && (ideRootPath.equals(currentDir))) {
        // prevent that during tests we traverse to the real IDE project of IDEasy developer
        currentDir = null;
      }
    }

    // detection completed, initializing variables
    this.ideRoot = findIdeRoot(currentDir);

    setCwd(workingDirectory, workspace, currentDir);

    if (this.ideRoot != null) {
      Path tempDownloadPath = getTempDownloadPath();
      if (Files.isDirectory(tempDownloadPath)) {
        // TODO delete all files older than 1 day here...
      } else {
        this.fileAccess.mkdirs(tempDownloadPath);
      }
    }

    this.defaultToolRepository = new DefaultToolRepository(this);
    this.mavenRepository = new MavenRepository(this);
  }

  private Path findIdeRoot(Path ideHomePath) {

    Path ideRootPath = null;
    if (ideHomePath != null) {
      ideRootPath = ideHomePath.getParent();
    } else if (!isTest()) {
      ideRootPath = getIdeRootPathFromEnv();
    }
    return ideRootPath;
  }

  /**
   * @return the {@link #getIdeRoot() IDE_ROOT} from the system environment.
   */
  protected Path getIdeRootPathFromEnv() {

    String root = getSystem().getEnv(IdeVariables.IDE_ROOT.getName());
    if (root != null) {
      Path rootPath = Path.of(root);
      if (Files.isDirectory(rootPath)) {
        return rootPath;
      }
    }
    return null;
  }

  @Override
  public void setCwd(Path userDir, String workspace, Path ideHome) {

    this.cwd = userDir;
    this.workspaceName = workspace;
    this.ideHome = ideHome;
    if (ideHome == null) {
      this.workspacePath = null;
      this.confPath = null;
      this.settingsPath = null;
      this.pluginsPath = null;
    } else {
      this.workspacePath = this.ideHome.resolve(FOLDER_WORKSPACES).resolve(this.workspaceName);
      this.confPath = this.ideHome.resolve(FOLDER_CONF);
      this.settingsPath = this.ideHome.resolve(FOLDER_SETTINGS);
      this.settingsCommitIdPath = this.ideHome.resolve(IdeContext.SETTINGS_COMMIT_ID);
      this.pluginsPath = this.ideHome.resolve(FOLDER_PLUGINS);
    }
    if (isTest()) {
      // only for testing...
      if (this.ideHome == null) {
        this.userHome = Path.of("/non-existing-user-home-for-testing");
      } else {
        this.userHome = this.ideHome.resolve("home");
      }
    } else {
      this.userHome = Path.of(getSystem().getProperty("user.home"));
    }
    this.userHomeIde = this.userHome.resolve(FOLDER_DOT_IDE);
    this.downloadPath = this.userHome.resolve("Downloads/ide");

    this.path = computeSystemPath();
  }

  private String getMessageIdeHomeFound() {

    return "IDE environment variables have been set for " + this.ideHome + " in workspace " + this.workspaceName;
  }

  private String getMessageNotInsideIdeProject() {

    return "You are not inside an IDE project: " + this.cwd;
  }

  private String getMessageIdeRootNotFound() {

    String root = getSystem().getEnv("IDE_ROOT");
    if (root == null) {
      return "The environment variable IDE_ROOT is undefined. Please reinstall IDEasy or manually repair IDE_ROOT variable.";
    } else {
      return "The environment variable IDE_ROOT is pointing to an invalid path " + root + ". Please reinstall IDEasy or manually repair IDE_ROOT variable.";
    }
  }

  /**
   * @return {@code true} if this is a test context for JUnits, {@code false} otherwise.
   */
  public boolean isTest() {

    return false;
  }

  protected SystemPath computeSystemPath() {

    return new SystemPath(this);
  }

  private boolean isIdeHome(Path dir) {

    if (!Files.isDirectory(dir.resolve("workspaces"))) {
      return false;
    } else if (!Files.isDirectory(dir.resolve("settings"))) {
      return false;
    }
    return true;
  }

  private EnvironmentVariables createVariables() {

    AbstractEnvironmentVariables system = createSystemVariables();
    AbstractEnvironmentVariables user = system.extend(this.userHomeIde, EnvironmentVariablesType.USER);
    AbstractEnvironmentVariables settings = user.extend(this.settingsPath, EnvironmentVariablesType.SETTINGS);
    AbstractEnvironmentVariables workspace = settings.extend(this.workspacePath, EnvironmentVariablesType.WORKSPACE);
    AbstractEnvironmentVariables conf = workspace.extend(this.confPath, EnvironmentVariablesType.CONF);
    return conf.resolved();
  }

  protected AbstractEnvironmentVariables createSystemVariables() {

    return EnvironmentVariables.ofSystem(this);
  }

  @Override
  public SystemInfo getSystemInfo() {

    return this.systemInfo;
  }

  @Override
  public FileAccess getFileAccess() {

    // currently FileAccess contains download method and requires network proxy to be configured. Maybe download should be moved to its own interface/class
    configureNetworkProxy();
    return this.fileAccess;
  }

  @Override
  public CommandletManager getCommandletManager() {

    return this.commandletManager;
  }

  @Override
  public ToolRepository getDefaultToolRepository() {

    return this.defaultToolRepository;
  }

  @Override
  public MavenRepository getMavenToolRepository() {

    return this.mavenRepository;
  }

  @Override
  public CustomToolRepository getCustomToolRepository() {

    if (this.customToolRepository == null) {
      this.customToolRepository = CustomToolRepositoryImpl.of(this);
    }
    return this.customToolRepository;
  }

  @Override
  public Path getIdeHome() {

    return this.ideHome;
  }

  @Override
  public String getProjectName() {

    if (this.ideHome != null) {
      return this.ideHome.getFileName().toString();
    }
    return "";
  }

  @Override
  public VersionIdentifier getProjectVersion() {

    if (this.ideHome != null) {
      Path versionFile = this.ideHome.resolve(IdeContext.FILE_SOFTWARE_VERSION);
      if (Files.exists(versionFile)) {
        String version = this.fileAccess.readFileContent(versionFile).trim();
        return VersionIdentifier.of(version);
      }
    }
    return IdeMigrator.START_VERSION;
  }

  @Override
  public void setProjectVersion(VersionIdentifier version) {

    if (this.ideHome == null) {
      throw new IllegalStateException("IDE_HOME not available!");
    }
    Objects.requireNonNull(version);
    Path versionFile = this.ideHome.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    this.fileAccess.writeFileContent(version.toString(), versionFile);
  }

  @Override
  public Path getIdeRoot() {

    return this.ideRoot;
  }

  @Override
  public Path getIdePath() {

    Path myIdeRoot = getIdeRoot();
    if (myIdeRoot == null) {
      return null;
    }
    return myIdeRoot.resolve(FOLDER_UNDERSCORE_IDE);
  }

  @Override
  public Path getCwd() {

    return this.cwd;
  }

  @Override
  public Path getTempPath() {

    Path idePath = getIdePath();
    if (idePath == null) {
      return null;
    }
    return idePath.resolve("tmp");
  }

  @Override
  public Path getTempDownloadPath() {

    Path tmp = getTempPath();
    if (tmp == null) {
      return null;
    }
    return tmp.resolve(FOLDER_DOWNLOADS);
  }

  @Override
  public Path getUserHome() {

    return this.userHome;
  }

  @Override
  public Path getUserHomeIde() {

    return this.userHomeIde;
  }

  @Override
  public Path getSettingsPath() {

    return this.settingsPath;
  }

  @Override
  public Path getSettingsGitRepository() {

    Path settingsPath = getSettingsPath();
    // check whether the settings path has a .git folder only if its not a symbolic link or junction
    if ((settingsPath != null) && !Files.exists(settingsPath.resolve(".git")) && !isSettingsRepositorySymlinkOrJunction()) {
      error("Settings repository exists but is not a git repository.");
      return null;
    }
    return settingsPath;
  }

  @Override
  public boolean isSettingsRepositorySymlinkOrJunction() {

    Path settingsPath = getSettingsPath();
    if (settingsPath == null) {
      return false;
    }
    return Files.isSymbolicLink(settingsPath) || getFileAccess().isJunction(settingsPath);
  }

  @Override
  public Path getSettingsCommitIdPath() {

    return this.settingsCommitIdPath;
  }

  @Override
  public Path getConfPath() {

    return this.confPath;
  }

  @Override
  public Path getSoftwarePath() {

    if (this.ideHome == null) {
      return null;
    }
    return this.ideHome.resolve(FOLDER_SOFTWARE);
  }

  @Override
  public Path getSoftwareExtraPath() {

    Path softwarePath = getSoftwarePath();
    if (softwarePath == null) {
      return null;
    }
    return softwarePath.resolve(FOLDER_EXTRA);
  }

  @Override
  public Path getSoftwareRepositoryPath() {

    Path idePath = getIdePath();
    if (idePath == null) {
      return null;
    }
    return idePath.resolve(FOLDER_SOFTWARE);
  }

  @Override
  public Path getPluginsPath() {

    return this.pluginsPath;
  }

  @Override
  public String getWorkspaceName() {

    return this.workspaceName;
  }

  @Override
  public Path getWorkspacePath() {

    return this.workspacePath;
  }

  @Override
  public Path getDownloadPath() {

    return this.downloadPath;
  }

  @Override
  public Path getUrlsPath() {

    Path idePath = getIdePath();
    if (idePath == null) {
      return null;
    }
    return idePath.resolve(FOLDER_URLS);
  }

  @Override
  public Path getToolRepositoryPath() {

    Path idePath = getIdePath();
    if (idePath == null) {
      return null;
    }
    return idePath.resolve(FOLDER_SOFTWARE);
  }

  @Override
  public SystemPath getPath() {

    return this.path;
  }

  @Override
  public EnvironmentVariables getVariables() {

    if (this.variables == null) {
      this.variables = createVariables();
    }
    return this.variables;
  }

  @Override
  public UrlMetadata getUrls() {

    if (this.urlMetadata == null) {
      if (!isTest()) {
        getGitContext().pullOrCloneAndResetIfNeeded(IDE_URLS_GIT, getUrlsPath(), null);
      }
      this.urlMetadata = new UrlMetadata(this);
    }
    return this.urlMetadata;
  }

  @Override
  public boolean isQuietMode() {

    return this.startContext.isQuietMode();
  }

  @Override
  public boolean isBatchMode() {

    return this.startContext.isBatchMode();
  }

  @Override
  public boolean isForceMode() {

    return this.startContext.isForceMode();
  }

  @Override
  public boolean isForcePull() {

    return this.startContext.isForcePull();
  }

  @Override
  public boolean isForcePlugins() {

    return this.startContext.isForcePlugins();
  }

  @Override
  public boolean isForceRepositories() {

    return this.startContext.isForceRepositories();
  }

  @Override
  public boolean isOfflineMode() {

    return this.startContext.isOfflineMode();
  }

  @Override
  public boolean isSkipUpdatesMode() {

    return this.startContext.isSkipUpdatesMode();
  }

  @Override
  public boolean isOnline() {

    if (this.online == null) {
      configureNetworkProxy();
      // we currently assume we have only a CLI process that runs shortly
      // therefore we run this check only once to save resources when this method is called many times
      try {
        int timeout = 1000;
        //open a connection to github.com and try to retrieve data
        //getContent fails if there is no connection
        URLConnection connection = new URL("https://www.github.com").openConnection();
        connection.setConnectTimeout(timeout);
        connection.getContent();
        this.online = Boolean.TRUE;
      } catch (Exception ignored) {
        this.online = Boolean.FALSE;
      }
    }
    return this.online.booleanValue();
  }

  private void configureNetworkProxy() {

    if (this.networkProxy == null) {
      this.networkProxy = new NetworkProxy(this);
      this.networkProxy.configure();
    }
  }

  @Override
  public Locale getLocale() {

    Locale locale = this.startContext.getLocale();
    if (locale == null) {
      locale = Locale.getDefault();
    }
    return locale;
  }

  @Override
  public DirectoryMerger getWorkspaceMerger() {

    if (this.workspaceMerger == null) {
      this.workspaceMerger = new DirectoryMerger(this);
    }
    return this.workspaceMerger;
  }

  /**
   * @return the {@link #getDefaultExecutionDirectory() default execution directory} in which a command process is executed.
   */
  @Override
  public Path getDefaultExecutionDirectory() {

    return this.defaultExecutionDirectory;
  }

  /**
   * @param defaultExecutionDirectory new value of {@link #getDefaultExecutionDirectory()}.
   */
  public void setDefaultExecutionDirectory(Path defaultExecutionDirectory) {

    if (defaultExecutionDirectory != null) {
      this.defaultExecutionDirectory = defaultExecutionDirectory;
    }
  }

  @Override
  public GitContext getGitContext() {

    return new GitContextImpl(this);
  }

  @Override
  public ProcessContext newProcess() {

    ProcessContext processContext = createProcessContext();
    if (this.defaultExecutionDirectory != null) {
      processContext.directory(this.defaultExecutionDirectory);
    }
    return processContext;
  }

  @Override
  public IdeSystem getSystem() {

    if (this.system == null) {
      this.system = new IdeSystemImpl(this);
    }
    return this.system;
  }

  /**
   * @return a new instance of {@link ProcessContext}.
   * @see #newProcess()
   */
  protected ProcessContext createProcessContext() {

    return new ProcessContextImpl(this);
  }

  @Override
  public IdeSubLogger level(IdeLogLevel level) {

    return this.startContext.level(level);
  }

  @Override
  public void logIdeHomeAndRootStatus() {

    if (this.ideRoot != null) {
      success("IDE_ROOT is set to {}", this.ideRoot);
    }
    if (this.ideHome == null) {
      warning(getMessageNotInsideIdeProject());
    } else {
      success("IDE_HOME is set to {}", this.ideHome);
    }
  }

  @Override
  public String askForInput(String message, String defaultValue) {

    if (!message.isBlank()) {
      info(message);
    }
    if (isBatchMode()) {
      if (isForceMode() || isForcePull()) {
        return defaultValue;
      } else {
        throw new CliAbortException();
      }
    }
    String input = readLine().trim();
    return input.isEmpty() ? defaultValue : input;
  }

  @Override
  public String askForInput(String message) {

    String input;
    do {
      info(message);
      input = readLine().trim();
    } while (input.isEmpty());

    return input;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <O> O question(String question, O... options) {

    assert (options.length >= 2);
    interaction(question);
    Map<String, O> mapping = new HashMap<>(options.length);
    int i = 0;
    for (O option : options) {
      i++;
      String key = "" + option;
      addMapping(mapping, key, option);
      String numericKey = Integer.toString(i);
      if (numericKey.equals(key)) {
        trace("Options should not be numeric: " + key);
      } else {
        addMapping(mapping, numericKey, option);
      }
      interaction("Option " + numericKey + ": " + key);
    }
    O option = null;
    if (isBatchMode()) {
      if (isForceMode() || isForcePull()) {
        option = options[0];
        interaction("" + option);
      }
    } else {
      while (option == null) {
        String answer = readLine();
        option = mapping.get(answer);
        if (option == null) {
          warning("Invalid answer: '" + answer + "' - please try again.");
        }
      }
    }
    return option;
  }

  /**
   * @return the input from the end-user (e.g. read from the console).
   */
  protected abstract String readLine();

  private static <O> void addMapping(Map<String, O> mapping, String key, O option) {

    O duplicate = mapping.put(key, option);
    if (duplicate != null) {
      throw new IllegalArgumentException("Duplicated option " + key);
    }
  }

  @Override
  public Step getCurrentStep() {

    return this.currentStep;
  }

  @Override
  public StepImpl newStep(boolean silent, String name, Object... parameters) {

    this.currentStep = new StepImpl(this, this.currentStep, name, silent, parameters);
    return this.currentStep;
  }

  /**
   * Internal method to end the running {@link Step}.
   *
   * @param step the current {@link Step} to end.
   */
  public void endStep(StepImpl step) {

    if (step == this.currentStep) {
      this.currentStep = this.currentStep.getParent();
    } else {
      String currentStepName = "null";
      if (this.currentStep != null) {
        currentStepName = this.currentStep.getName();
      }
      warning("endStep called with wrong step '{}' but expected '{}'", step.getName(), currentStepName);
    }
  }

  /**
   * Finds the matching {@link Commandlet} to run, applies {@link CliArguments} to its {@link Commandlet#getProperties() properties} and will execute it.
   *
   * @param arguments the {@link CliArgument}.
   * @return the return code of the execution.
   */
  public int run(CliArguments arguments) {

    CliArgument current = arguments.current();
    assert (this.currentStep == null);
    boolean supressStepSuccess = false;
    StepImpl step = newStep(true, "ide", (Object[]) current.asArray());
    Iterator<Commandlet> commandletIterator = this.commandletManager.findCommandlet(arguments, null);
    Commandlet cmd = null;
    ValidationResult result = null;
    try {
      while (commandletIterator.hasNext()) {
        cmd = commandletIterator.next();
        result = applyAndRun(arguments.copy(), cmd);
        if (result.isValid()) {
          supressStepSuccess = cmd.isSuppressStepSuccess();
          step.success();
          return ProcessResult.SUCCESS;
        }
      }
      this.startContext.activateLogging();
      if (result != null) {
        error(result.getErrorMessage());
      }
      step.error("Invalid arguments: {}", current.getArgs());
      HelpCommandlet help = this.commandletManager.getCommandlet(HelpCommandlet.class);
      if (cmd != null) {
        help.commandlet.setValue(cmd);
      }
      help.run();
      return 1;
    } catch (Throwable t) {
      this.startContext.activateLogging();
      step.error(t, true);
      throw t;
    } finally {
      step.close();
      assert (this.currentStep == null);
      step.logSummary(supressStepSuccess);
    }
  }

  /**
   * @param cmd the potential {@link Commandlet} to {@link #apply(CliArguments, Commandlet) apply} and {@link Commandlet#run() run}.
   * @return {@code true} if the given {@link Commandlet} matched and did {@link Commandlet#run() run} successfully, {@code false} otherwise (the
   *     {@link Commandlet} did not match and we have to try a different candidate).
   */
  private ValidationResult applyAndRun(CliArguments arguments, Commandlet cmd) {

    IdeLogLevel previousLogLevel = null;
    cmd.reset();
    ValidationResult result = apply(arguments, cmd);
    if (result.isValid()) {
      result = cmd.validate();
    }
    if (result.isValid()) {
      debug("Running commandlet {}", cmd);
      if (cmd.isIdeHomeRequired() && (this.ideHome == null)) {
        throw new CliException(getMessageNotInsideIdeProject(), ProcessResult.NO_IDE_HOME);
      } else if (cmd.isIdeRootRequired() && (this.ideRoot == null)) {
        throw new CliException(getMessageIdeRootNotFound(), ProcessResult.NO_IDE_ROOT);
      }
      try {
        if (cmd.isProcessableOutput()) {
          if (!debug().isEnabled()) {
            // unless --debug or --trace was supplied, processable output commandlets will disable all log-levels except INFO to prevent other logs interfere
            previousLogLevel = this.startContext.setLogLevel(IdeLogLevel.PROCESSABLE);
          }
          this.startContext.activateLogging();
        } else {
          this.startContext.activateLogging();
          verifyIdeRoot();
          if (cmd.isIdeHomeRequired()) {
            debug(getMessageIdeHomeFound());
          }
          Path settingsRepository = getSettingsGitRepository();
          if (settingsRepository != null) {
            if (getGitContext().isRepositoryUpdateAvailable(settingsRepository, getSettingsCommitIdPath()) || (
                getGitContext().fetchIfNeeded(settingsRepository) && getGitContext().isRepositoryUpdateAvailable(
                    settingsRepository, getSettingsCommitIdPath()))) {
              if (isSettingsRepositorySymlinkOrJunction()) {
                interaction(
                    "Updates are available for the settings repository. Please pull the latest changes by yourself or by calling \"ide -f update\" to apply them.");

              } else {
                interaction(
                    "Updates are available for the settings repository. If you want to apply the latest changes, call \"ide update\"");
              }
            }
          }
        }
        boolean success = ensureLicenseAgreement(cmd);
        if (!success) {
          return ValidationResultValid.get();
        }
        cmd.run();
      } finally {
        if (previousLogLevel != null) {
          this.startContext.setLogLevel(previousLogLevel);
        }
      }
    } else {
      trace("Commandlet did not match");
    }
    return result;
  }

  private boolean ensureLicenseAgreement(Commandlet cmd) {

    if (isTest()) {
      return true; // ignore for tests
    }
    getFileAccess().mkdirs(this.userHomeIde);
    Path licenseAgreement = this.userHomeIde.resolve(FILE_LICENSE_AGREEMENT);
    if (Files.isRegularFile(licenseAgreement)) {
      return true; // success, license already accepted
    }
    if (cmd instanceof EnvironmentCommandlet) {
      // if the license was not accepted, "$(ideasy env --bash)" that is written into a variable prevents the user from seeing the question he is asked
      // in such situation the user could not open a bash terminal anymore and gets blocked what would really annoy the user so we exit here without doing or
      // printing anything anymore in such case.
      return false;
    }
    boolean logLevelInfoDisabled = !this.startContext.info().isEnabled();
    if (logLevelInfoDisabled) {
      this.startContext.setLogLevel(IdeLogLevel.INFO, true);
    }
    boolean logLevelInteractionDisabled = !this.startContext.interaction().isEnabled();
    if (logLevelInteractionDisabled) {
      this.startContext.setLogLevel(IdeLogLevel.INTERACTION, true);
    }
    StringBuilder sb = new StringBuilder(1180);
    sb.append(LOGO).append("""
        Welcome to IDEasy!
        This product (with its included 3rd party components) is open-source software and can be used free (also commercially).
        It supports automatic download and installation of arbitrary 3rd party tools.
        By default only open-source 3rd party tools are used (downloaded, installed, executed).
        But if explicitly configured, also commercial software that requires an additional license may be used.
        This happens e.g. if you configure "ultimate" edition of IntelliJ or "docker" edition of Docker (Docker Desktop).
        You are solely responsible for all risks implied by using this software.
        Before using IDEasy you need to read and accept the license agreement with all involved licenses.
        You will be able to find it online under the following URL:
        """).append(LICENSE_URL);
    if (this.ideRoot != null) {
      sb.append("\n\nAlso it is included in the documentation that you can find here:\n").
          append(getIdePath().resolve("IDEasy.pdf").toString()).append("\n");
    }
    info(sb.toString());
    askToContinue("Do you accept these terms of use and all license agreements?");

    sb.setLength(0);
    LocalDateTime now = LocalDateTime.now();
    sb.append("On ").append(DateTimeUtil.formatDate(now, false)).append(" at ").append(DateTimeUtil.formatTime(now))
        .append(" you accepted the IDEasy license.\n").append(LICENSE_URL);
    try {
      Files.writeString(licenseAgreement, sb);
    } catch (Exception e) {
      throw new RuntimeException("Failed to save license agreement!", e);
    }
    if (logLevelInfoDisabled) {
      this.startContext.setLogLevel(IdeLogLevel.INFO, false);
    }
    if (logLevelInteractionDisabled) {
      this.startContext.setLogLevel(IdeLogLevel.INTERACTION, false);
    }
    return true;
  }

  private void verifyIdeRoot() {

    if (!isTest()) {
      if (this.ideRoot == null) {
        warning("Variable IDE_ROOT is undefined. Please check your installation or run setup script again.");
      } else if (this.ideHome != null) {
        Path ideRootPath = getIdeRootPathFromEnv();
        if (!this.ideRoot.equals(ideRootPath)) {
          warning("Variable IDE_ROOT is set to '{}' but for your project '{}' the path '{}' would have been expected.", ideRootPath,
              this.ideHome.getFileName(), this.ideRoot);
        }
      }
    }
  }

  /**
   * @param arguments the {@link CliArguments#ofCompletion(String...) completion arguments}.
   * @param includeContextOptions to include the options of {@link ContextCommandlet}.
   * @return the {@link List} of {@link CompletionCandidate}s to suggest.
   */
  public List<CompletionCandidate> complete(CliArguments arguments, boolean includeContextOptions) {

    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(this);
    if (arguments.current().isStart()) {
      arguments.next();
    }
    if (includeContextOptions) {
      ContextCommandlet cc = new ContextCommandlet();
      for (Property<?> property : cc.getProperties()) {
        assert (property.isOption());
        property.apply(arguments, this, cc, collector);
      }
    }
    Iterator<Commandlet> commandletIterator = this.commandletManager.findCommandlet(arguments, collector);
    CliArgument current = arguments.current();
    if (current.isCompletion() && current.isCombinedShortOption()) {
      collector.add(current.get(), null, null, null);
    }
    arguments.next();
    while (commandletIterator.hasNext()) {
      Commandlet cmd = commandletIterator.next();
      if (!arguments.current().isEnd()) {
        completeCommandlet(arguments.copy(), cmd, collector);
      }
    }
    return collector.getSortedCandidates();
  }

  private void completeCommandlet(CliArguments arguments, Commandlet cmd, CompletionCandidateCollector collector) {

    trace("Trying to match arguments for auto-completion for commandlet {}", cmd.getName());
    Iterator<Property<?>> valueIterator = cmd.getValues().iterator();
    valueIterator.next(); // skip first property since this is the keyword property that already matched to find the commandlet
    List<Property<?>> properties = cmd.getProperties();
    // we are creating our own list of options and remove them when matched to avoid duplicate suggestions
    List<Property<?>> optionProperties = new ArrayList<>(properties.size());
    for (Property<?> property : properties) {
      if (property.isOption()) {
        optionProperties.add(property);
      }
    }
    CliArgument currentArgument = arguments.current();
    while (!currentArgument.isEnd()) {
      trace("Trying to match argument '{}'", currentArgument);
      if (currentArgument.isOption() && !arguments.isEndOptions()) {
        if (currentArgument.isCompletion()) {
          Iterator<Property<?>> optionIterator = optionProperties.iterator();
          while (optionIterator.hasNext()) {
            Property<?> option = optionIterator.next();
            boolean success = option.apply(arguments, this, cmd, collector);
            if (success) {
              optionIterator.remove();
              arguments.next();
            }
          }
        } else {
          Property<?> option = cmd.getOption(currentArgument.get());
          if (option != null) {
            arguments.next();
            boolean removed = optionProperties.remove(option);
            if (!removed) {
              option = null;
            }
          }
          if (option == null) {
            trace("No such option was found.");
            return;
          }
        }
      } else {
        if (valueIterator.hasNext()) {
          Property<?> valueProperty = valueIterator.next();
          boolean success = valueProperty.apply(arguments, this, cmd, collector);
          if (!success) {
            trace("Completion cannot match any further.");
            return;
          }
        } else {
          trace("No value left for completion.");
          return;
        }
      }
      currentArgument = arguments.current();
    }
  }

  /**
   * @param arguments the {@link CliArguments} to apply. Will be {@link CliArguments#next() consumed} as they are matched. Consider passing a
   *     {@link CliArguments#copy() copy} as needed.
   * @param cmd the potential {@link Commandlet} to match.
   * @return the {@link ValidationResult} telling if the {@link CliArguments} can be applied successfully or if validation errors ocurred.
   */
  public ValidationResult apply(CliArguments arguments, Commandlet cmd) {

    trace("Trying to match arguments to commandlet {}", cmd.getName());
    CliArgument currentArgument = arguments.current();
    Iterator<Property<?>> propertyIterator = cmd.getValues().iterator();
    Property<?> property = null;
    if (propertyIterator.hasNext()) {
      property = propertyIterator.next();
    }
    while (!currentArgument.isEnd()) {
      trace("Trying to match argument '{}'", currentArgument);
      Property<?> currentProperty = property;
      if (!arguments.isEndOptions()) {
        Property<?> option = cmd.getOption(currentArgument.getKey());
        if (option != null) {
          currentProperty = option;
        }
      }
      if (currentProperty == null) {
        trace("No option or next value found");
        ValidationState state = new ValidationState(null);
        state.addErrorMessage("No matching property found");
        return state;
      }
      trace("Next property candidate to match argument is {}", currentProperty);
      if (currentProperty == property) {
        if (!property.isMultiValued()) {
          if (propertyIterator.hasNext()) {
            property = propertyIterator.next();
          } else {
            property = null;
          }
        }
        if ((property != null) && property.isValue() && property.isMultiValued()) {
          arguments.stopSplitShortOptions();
        }
      }
      boolean matches = currentProperty.apply(arguments, this, cmd, null);
      if (!matches) {
        ValidationState state = new ValidationState(null);
        state.addErrorMessage("No matching property found");
        return state;
      }
      currentArgument = arguments.current();
    }
    return ValidationResultValid.get();
  }

  @Override
  public String findBash() {

    String bash = "bash";
    if (SystemInfoImpl.INSTANCE.isWindows()) {
      bash = findBashOnWindows();
    }

    return bash;
  }

  private String findBashOnWindows() {

    // Check if Git Bash exists in the default location
    Path defaultPath = Path.of("C:\\Program Files\\Git\\bin\\bash.exe");
    if (Files.exists(defaultPath)) {
      return defaultPath.toString();
    }

    // If not found in the default location, try the registry query
    String[] bashVariants = { "GitForWindows", "Cygwin\\setup" };
    String[] registryKeys = { "HKEY_LOCAL_MACHINE", "HKEY_CURRENT_USER" };
    String regQueryResult;
    for (String bashVariant : bashVariants) {
      for (String registryKey : registryKeys) {
        String toolValueName = ("GitForWindows".equals(bashVariant)) ? "InstallPath" : "rootdir";
        String command = "reg query " + registryKey + "\\Software\\" + bashVariant + "  /v " + toolValueName + " 2>nul";

        try {
          Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
              output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
              return null;
            }

            regQueryResult = output.toString();
            if (regQueryResult != null) {
              int index = regQueryResult.indexOf("REG_SZ");
              if (index != -1) {
                String path = regQueryResult.substring(index + "REG_SZ".length()).trim();
                return path + "\\bin\\bash.exe";
              }
            }

          }
        } catch (Exception e) {
          return null;
        }
      }
    }
    // no bash found
    return null;
  }

  @Override
  public WindowsPathSyntax getPathSyntax() {

    return this.pathSyntax;
  }

  /**
   * @param pathSyntax new value of {@link #getPathSyntax()}.
   */
  public void setPathSyntax(WindowsPathSyntax pathSyntax) {

    this.pathSyntax = pathSyntax;
  }

  /**
   * @return the {@link IdeStartContextImpl}.
   */
  public IdeStartContextImpl getStartContext() {

    return startContext;
  }

  /**
   * @return the {@link WindowsHelper}.
   */
  public final WindowsHelper getWindowsHelper() {

    if (this.windowsHelper == null) {
      this.windowsHelper = createWindowsHelper();
    }
    return this.windowsHelper;
  }

  /**
   * @return the new {@link WindowsHelper} instance.
   */
  protected WindowsHelper createWindowsHelper() {

    return new WindowsHelperImpl(this);
  }

  /**
   * Reloads this context and re-initializes the {@link #getVariables() variables}.
   */
  public void reload() {

    this.variables = null;
    this.customToolRepository = null;
  }

  @Override
  public void writeVersionFile(VersionIdentifier version, Path installationPath) {

    assert (Files.isDirectory(installationPath));
    Path versionFile = installationPath.resolve(FILE_SOFTWARE_VERSION);
    getFileAccess().writeFileContent(version.toString(), versionFile);
  }

}
