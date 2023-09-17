package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.commandlet.CommandletManagerImpl;
import com.devonfw.tools.ide.common.SystemInfo;
import com.devonfw.tools.ide.common.SystemInfoImpl;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.log.IdeSubLoggerNone;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.repo.CustomToolRepository;
import com.devonfw.tools.ide.repo.CustomToolRepositoryImpl;
import com.devonfw.tools.ide.repo.DefaultToolRepository;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Abstract base implementation of {@link IdeContext}.
 */
public abstract class AbstractIdeContext implements IdeContext {

  private final Map<IdeLogLevel, IdeSubLogger> loggers;

  private final Path ideHome;

  private final Path ideRoot;

  private final Path confPath;

  private final Path settingsPath;

  private final Path softwarePath;

  private final Path softwareRepositoryPath;

  private final Path workspacePath;

  private final String workspaceName;

  private final Path urlsPath;

  private final Path tempPath;

  private final Path tempDownloadPath;

  private final Path cwd;

  private final Path downloadPath;

  private final Path toolRepository;

  private final Path userHome;

  private final Path userHomeIde;

  private final SystemPath path;

  private final SystemInfo systemInfo;

  private final EnvironmentVariables variables;

  private final FileAccess fileAccess;

  private final CommandletManager commandletManager;

  private final ToolRepository defaultToolRepository;

  private final CustomToolRepository customToolRepository;

  private boolean offlineMode;

  private boolean forceMode;

  private boolean batchMode;

  private boolean quietMode;

  private Locale locale;

  private UrlMetadata urlMetadata;

  /**
   * The constructor.
   *
   * @param minLogLevel the minimum {@link IdeLogLevel} to enable. Should be {@link IdeLogLevel#INFO} by default.
   * @param factory the {@link Function} to create {@link IdeSubLogger} per {@link IdeLogLevel}.
   * @param userDir the optional {@link Path} to current working directory.
   */
  public AbstractIdeContext(IdeLogLevel minLogLevel, Function<IdeLogLevel, IdeSubLogger> factory, Path userDir) {

    super();
    this.loggers = new HashMap<>();
    for (IdeLogLevel level : IdeLogLevel.values()) {
      IdeSubLogger logger;
      if (level.ordinal() < minLogLevel.ordinal()) {
        logger = new IdeSubLoggerNone(level);
      } else {
        logger = factory.apply(level);
      }
      this.loggers.put(level, logger);
    }
    this.systemInfo = new SystemInfoImpl();
    this.commandletManager = CommandletManagerImpl.of(this);
    this.fileAccess = new FileAccessImpl(this);
    String workspace = WORKSPACE_MAIN;
    if (userDir == null) {
      this.cwd = Paths.get(System.getProperty("user.dir"));
    } else {
      this.cwd = userDir;
    }
    // detect IDE_HOME and WORKSPACE
    Path currentDir = this.cwd;
    String name1 = "";
    String name2 = "";
    while (currentDir != null) {
      trace("Looking for IDE_HOME in {}", currentDir);
      if (isIdeHome(currentDir)) {
        if (FOLDER_WORKSPACES.equals(name1)) {
          workspace = name2;
        }
        break;
      }
      name2 = name1;
      int nameCount = currentDir.getNameCount();
      name1 = currentDir.getName(nameCount - 1).toString();
      currentDir = getParentPath(currentDir);
    }
    // detection completed, initializing variables
    this.ideHome = currentDir;
    this.workspaceName = workspace;
    if (this.ideHome == null) {
      info(getMessageIdeHomeNotFound());
      this.workspacePath = null;
      this.ideRoot = null;
      this.confPath = null;
      this.settingsPath = null;
      this.softwarePath = null;
    } else {
      debug(getMessageIdeHomeFound());
      this.workspacePath = this.ideHome.resolve(FOLDER_WORKSPACES).resolve(this.workspaceName);
      Path ideRootPath = this.ideHome.getParent();
      String root = null;
      if (!isTest()) {
        root = System.getenv("IDE_ROOT");
      }
      if (root != null) {
        Path rootPath = Paths.get(root);
        if (Files.isDirectory(rootPath)) {
          if (!ideRootPath.equals(rootPath)) {
            warning("Variable IDE_ROOT is set to '{}' but for your project '{}' would have been expected.");
            ideRootPath = rootPath;
          }
          ideRootPath = this.ideHome.getParent();
        } else {
          warning("Variable IDE_ROOT is not set to a valid directory '{}'." + root);
          ideRootPath = null;
        }
      }
      this.ideRoot = ideRootPath;
      this.confPath = this.ideHome.resolve(FOLDER_CONF);
      this.settingsPath = this.ideHome.resolve(FOLDER_SETTINGS);
      this.softwarePath = this.ideHome.resolve(FOLDER_SOFTWARE);
    }
    if (this.ideRoot == null) {
      this.toolRepository = null;
      this.urlsPath = null;
      this.tempPath = null;
      this.tempDownloadPath = null;
      this.softwareRepositoryPath = null;
    } else {
      Path ideBase = this.ideRoot.resolve(FOLDER_IDE);
      this.toolRepository = ideBase.resolve("software");
      this.urlsPath = ideBase.resolve("urls");
      this.tempPath = ideBase.resolve("tmp");
      this.tempDownloadPath = this.tempPath.resolve(FOLDER_DOWNLOADS);
      this.softwareRepositoryPath = ideBase.resolve(FOLDER_SOFTWARE);
      if (Files.isDirectory(this.tempPath)) {
        // TODO delete all files older than 1 day here...
      } else {
        this.fileAccess.mkdirs(this.tempDownloadPath);
      }
    }
    if (isTest()) {
      // only for testing...
      this.userHome = this.ideHome.resolve("home");
    } else {
      this.userHome = Paths.get(System.getProperty("user.home"));
    }
    this.userHomeIde = this.userHome.resolve(".ide");
    this.downloadPath = this.userHome.resolve("Downloads/ide");
    this.variables = createVariables();
    this.path = computeSystemPath();
    this.defaultToolRepository = new DefaultToolRepository(this);
    this.customToolRepository = CustomToolRepositoryImpl.of(this);
  }

  private String getMessageIdeHomeFound() {

    return "IDE environment variables have been set for " + this.ideHome + " in workspace " + this.workspaceName;
  }

  private String getMessageIdeHomeNotFound() {

    return "You are not inside an IDE installation: " + this.cwd;
  }

  /**
   * @return the status message about the {@link #getIdeHome() IDE_HOME} detection and environment variable
   *         initialization.
   */
  public String getMessageIdeHome() {

    if (this.ideHome == null) {
      return getMessageIdeHomeNotFound();
    }
    return getMessageIdeHomeFound();
  }

  /**
   * @return {@code true} if this is a test context for JUnits, {@code false} otherwise.
   */
  protected boolean isTest() {

    return false;
  }

  private SystemPath computeSystemPath() {

    String systemPath = System.getenv(IdeVariables.PATH.getName());
    return new SystemPath(systemPath, this.softwarePath);
  }

  private boolean isIdeHome(Path dir) {

    if (!Files.isRegularFile(dir.resolve("setup"))) {
      return false;
    } else if (!Files.isDirectory(dir.resolve("scripts"))) {
      return false;
    } else if (dir.toString().endsWith("/scripts/src/main/resources")) {
      // TODO does this still make sense for our new Java based product?
      return false;
    }
    return true;
  }

  private Path getParentPath(Path dir) {

    try {
      Path linkDir = dir.toRealPath();
      if (!dir.equals(linkDir)) {
        return linkDir;
      } else {
        return dir.getParent();
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

  }

  private EnvironmentVariables createVariables() {

    AbstractEnvironmentVariables system = EnvironmentVariables.ofSystem(this);
    AbstractEnvironmentVariables user = extendVariables(system, this.userHomeIde, EnvironmentVariablesType.USER);
    AbstractEnvironmentVariables settings = extendVariables(user, this.settingsPath, EnvironmentVariablesType.SETTINGS);
    // TODO should we keep this workspace properties? Was this feature ever used?
    AbstractEnvironmentVariables workspace = extendVariables(settings, this.workspacePath,
        EnvironmentVariablesType.WORKSPACE);
    AbstractEnvironmentVariables conf = extendVariables(workspace, this.confPath, EnvironmentVariablesType.CONF);
    return conf.resolved();
  }

  private AbstractEnvironmentVariables extendVariables(AbstractEnvironmentVariables envVariables, Path propertiesPath,
      EnvironmentVariablesType type) {

    Path propertiesFile = null;
    if (propertiesPath == null) {
      trace("Configuration directory for type {} does not exist.", type);
    } else if (Files.isDirectory(propertiesPath)) {
      propertiesFile = propertiesPath.resolve(EnvironmentVariables.DEFAULT_PROPERTIES);
      boolean legacySupport = (type != EnvironmentVariablesType.USER);
      if (legacySupport && !Files.exists(propertiesFile)) {
        Path legacyFile = propertiesPath.resolve(EnvironmentVariables.LEGACY_PROPERTIES);
        if (Files.exists(legacyFile)) {
          propertiesFile = legacyFile;
        }
      }
    } else {
      debug("Configuration directory {} does not exist.", propertiesPath);
    }
    return envVariables.extend(propertiesFile, type);
  }

  @Override
  public SystemInfo getSystemInfo() {

    return this.systemInfo;
  }

  @Override
  public FileAccess getFileAccess() {

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
  public CustomToolRepository getCustomToolRepository() {

    return this.customToolRepository;
  }

  @Override
  public Path getIdeHome() {

    return this.ideHome;
  }

  @Override
  public Path getIdeRoot() {

    return this.ideRoot;
  }

  @Override
  public Path getCwd() {

    return this.cwd;
  }

  @Override
  public Path getTempPath() {

    return this.tempPath;
  }

  @Override
  public Path getTempDownloadPath() {

    return this.tempDownloadPath;
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
  public Path getConfPath() {

    return this.confPath;
  }

  @Override
  public Path getSoftwarePath() {

    return this.softwarePath;
  }

  @Override
  public Path getSoftwareRepositoryPath() {

    return this.softwareRepositoryPath;
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

    return this.urlsPath;
  }

  @Override
  public Path getToolRepository() {

    return this.toolRepository;
  }

  @Override
  public SystemPath getPath() {

    return this.path;
  }

  @Override
  public EnvironmentVariables getVariables() {

    return this.variables;
  }

  @Override
  public UrlMetadata getUrls() {

    if (this.urlMetadata == null) {
      if (!isTest()) {
        gitPullOrClone(this.urlsPath, "https://github.com/devonfw/ide-urls.git");
      }
      this.urlMetadata = new UrlMetadata(this);
    }
    return this.urlMetadata;
  }

  @Override
  public boolean isQuietMode() {

    return this.quietMode;
  }

  /**
   * @param quietMode new value of {@link #isQuietMode()}.
   */
  public void setQuietMode(boolean quietMode) {

    this.quietMode = quietMode;
  }

  @Override
  public boolean isBatchMode() {

    return this.batchMode;
  }

  /**
   * @param batchMode new value of {@link #isBatchMode()}.
   */
  public void setBatchMode(boolean batchMode) {

    this.batchMode = batchMode;
  }

  @Override
  public boolean isForceMode() {

    return this.forceMode;
  }

  /**
   * @param forceMode new value of {@link #isForceMode()}.
   */
  public void setForceMode(boolean forceMode) {

    this.forceMode = forceMode;
  }

  @Override
  public boolean isOfflineMode() {

    return this.offlineMode;
  }

  /**
   * @param offlineMode new value of {@link #isOfflineMode()}.
   */
  public void setOfflineMode(boolean offlineMode) {

    this.offlineMode = offlineMode;
  }

  @Override
  public boolean isOnline() {

    boolean online = false;
    try {
      int timeout = 1000;
      online = InetAddress.getByName("github.com").isReachable(timeout);
    } catch (Exception e) {

    }
    return online;
  }

  @Override
  public Locale getLocale() {

    if (this.locale == null) {
      return Locale.getDefault();
    }
    return this.locale;
  }

  /**
   * @param locale new value of {@link #getLocale()}.
   */
  public void setLocale(Locale locale) {

    this.locale = locale;
  }

  @Override
  public ProcessContext newProcess() {

    ProcessContext processContext = new ProcessContextImpl(this);
    return processContext;
  }

  @Override
  public void gitPullOrClone(Path target, String gitRepoUrl) {

    Objects.requireNonNull(target);
    Objects.requireNonNull(gitRepoUrl);
    if (!gitRepoUrl.startsWith("http")) {
      throw new IllegalArgumentException("Invalid git URL '" + gitRepoUrl + "'!");
    }
    ProcessContext pc = newProcess().directory(target).executable("git");
    if (Files.isDirectory(target.resolve(".git"))) {
      ProcessResult result = pc.addArg("remote").run(true);
      List<String> remotes = result.getOut();
      if (remotes.isEmpty()) {
        String message = "This is a local git repo with no remote - if you did this for testing, you may continue...\n"
            + "Do you want to ignore the problem and continue anyhow?";
        askToContinue(message);
      } else {
        pc.errorHandling(ProcessErrorHandling.WARNING);
        result = pc.addArg("pull").run(false);
        if (!result.isSuccessful()) {
          String message = "Failed to update git repository at " + target;
          if (this.offlineMode) {
            warning(message);
            interaction("Continuing as we are in offline mode - results may be outdated!");
          } else {
            error(message);
            if (isOnline()) {
              error("See above error for details. If you have local changes, please stash or revert and retry.");
            } else {
              error(
                  "It seems you are offline - please ensure Internet connectivity and retry or activate offline mode (-o or --offline).");
            }
            askToContinue("Typically you should abort and fix the problem. Do you want to continue anyways?");
          }
        }
      }
    } else {
      String branch = null;
      int hashIndex = gitRepoUrl.indexOf("#");
      if (hashIndex != -1) {
        branch = gitRepoUrl.substring(hashIndex + 1);
        gitRepoUrl = gitRepoUrl.substring(0, hashIndex);
      }
      this.fileAccess.mkdirs(target);
      requireOnline("git clone of " + gitRepoUrl);
      pc.addArg("clone");
      if (isQuietMode()) {
        pc.addArg("-q");
      } else {
      }
      pc.addArgs("--recursive", gitRepoUrl, "--config", "core.autocrlf=false", ".");
      pc.run();
      if (branch != null) {
        pc.addArgs("checkout", branch);
        pc.run();
      }
    }
  }

  @Override
  public IdeSubLogger level(IdeLogLevel level) {

    IdeSubLogger logger = this.loggers.get(level);
    Objects.requireNonNull(logger);
    return logger;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <O> O question(String question, O... options) {

    assert (options.length >= 2);
    interaction(question);
    Map<String, O> mapping = new HashMap<>(options.length);
    int i = 1;
    for (O option : options) {
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
      if (isForceMode()) {
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

}
