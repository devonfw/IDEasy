package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.nio.file.attribute.FileTime;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.commandlet.CommandletManagerImpl;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.log.IdeSubLoggerNone;
import com.devonfw.tools.ide.merge.DirectoryMerger;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.Property;
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

  private final Path pluginsPath;

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

  private final DirectoryMerger workspaceMerger;

  private final Function<IdeLogLevel, IdeSubLogger> loggerFactory;

  private boolean offlineMode;

  private boolean forceMode;

  private boolean batchMode;

  private boolean quietMode;

  private Locale locale;

  private UrlMetadata urlMetadata;

  private static final Duration GIT_PULL_CACHE_DELAY_MILLIS = Duration.ofMillis(30 * 60 * 1000);

  /**
   * The constructor.
   *
   * @param minLogLevel the minimum {@link IdeLogLevel} to enable. Should be {@link IdeLogLevel#INFO} by default.
   * @param factory the {@link Function} to create {@link IdeSubLogger} per {@link IdeLogLevel}.
   * @param userDir the optional {@link Path} to current working directory.
   */
  public AbstractIdeContext(IdeLogLevel minLogLevel, Function<IdeLogLevel, IdeSubLogger> factory, Path userDir) {

    super();
    this.loggerFactory = factory;
    this.loggers = new HashMap<>();
    setLogLevel(minLogLevel);
    this.systemInfo = new SystemInfoImpl();
    this.commandletManager = new CommandletManagerImpl(this);
    this.fileAccess = new FileAccessImpl(this);
    String workspace = WORKSPACE_MAIN;
    if (userDir == null) {
      this.cwd = Paths.get(System.getProperty("user.dir"));
    } else {
      this.cwd = userDir.toAbsolutePath();
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
      if (nameCount >= 1) {
        name1 = currentDir.getName(nameCount - 1).toString();
      }
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
      this.pluginsPath = null;
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
      this.pluginsPath = this.ideHome.resolve(FOLDER_PLUGINS);
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
      if (this.ideHome == null) {
        this.userHome = Paths.get("/non-existing-user-home-for-testing");
      } else {
        this.userHome = this.ideHome.resolve("home");
      }
    } else {
      this.userHome = Paths.get(System.getProperty("user.home"));
    }
    this.userHomeIde = this.userHome.resolve(".ide");
    this.downloadPath = this.userHome.resolve("Downloads/ide");
    this.variables = createVariables();
    this.path = computeSystemPath();
    this.defaultToolRepository = new DefaultToolRepository(this);
    this.customToolRepository = CustomToolRepositoryImpl.of(this);
    this.workspaceMerger = new DirectoryMerger(this);
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
  public boolean isTest() {

    if (isMock()) {
      return true;
    }
    return false;
  }

  /**
   * @return {@code true} if this is a mock context for JUnits, {@code false} otherwise.
   */
  public boolean isMock() {

    return false;
  }

  private SystemPath computeSystemPath() {

    String systemPath = System.getenv(IdeVariables.PATH.getName());
    return new SystemPath(systemPath, this.softwarePath, this);
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

    return this.urlsPath;
  }

  @Override
  public Path getToolRepositoryPath() {

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
        gitPullOrCloneIfNeeded(this.urlsPath, "https://github.com/devonfw/ide-urls.git");
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
  public DirectoryMerger getWorkspaceMerger() {

    return this.workspaceMerger;
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
    ProcessContext pc = newProcess().directory(target).executable("git").withEnvVar("GIT_TERMINAL_PROMPT", "0");
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

  /**
   * Checks if the Git repository in the specified target folder needs an update by
   * inspecting the modification time of a magic file.
   *
   * @param urlsPath The Path to the Urls repository.
   * @param repoUrl The git remote URL of the Urls repository.
   */

  private void gitPullOrCloneIfNeeded(Path urlsPath, String repoUrl) {

    Path gitDirectory = urlsPath.resolve(".git");

    // Check if the .git directory exists
    if (Files.isDirectory(gitDirectory)) {
      Path magicFilePath = gitDirectory.resolve("HEAD");
      long currentTime = System.currentTimeMillis();
      // Get the modification time of the magic file
      long fileMTime;
      try {
        fileMTime = Files.getLastModifiedTime(magicFilePath).toMillis();
      } catch (IOException e) {
        throw new IllegalStateException("Could not read " + magicFilePath, e);
      }

      // Check if the file modification time is older than the delta threshold
      if ((currentTime - fileMTime > GIT_PULL_CACHE_DELAY_MILLIS.toMillis()) || isForceMode()) {
        gitPullOrClone(urlsPath, repoUrl);
        try {
          Files.setLastModifiedTime(magicFilePath, FileTime.fromMillis(currentTime));
        } catch (IOException e) {
          throw new IllegalStateException("Could not read or write in " + magicFilePath, e);
        }
      }
    } else {
      // If the .git directory does not exist, perform git clone
      gitPullOrClone(urlsPath, repoUrl);
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

  /**
   * Sets the log level.
   *
   * @param logLevel {@link IdeLogLevel}
   */
  public void setLogLevel(IdeLogLevel logLevel) {

    for (IdeLogLevel level : IdeLogLevel.values()) {
      IdeSubLogger logger;
      if (level.ordinal() < logLevel.ordinal()) {
        logger = new IdeSubLoggerNone(level);
      } else {
        logger = this.loggerFactory.apply(level);
      }
      this.loggers.put(level, logger);
    }
  }

  /**
   * Finds the matching {@link Commandlet} to run, applies {@link CliArguments} to its {@link Commandlet#getProperties()
   * properties} and will execute it.
   *
   * @param arguments the {@link CliArgument}.
   * @return the return code of the execution.
   */
  public int run(CliArguments arguments) {

    CliArgument current = arguments.current();
    if (!current.isEnd()) {
      String keyword = current.get();
      Commandlet firstCandidate = this.commandletManager.getCommandletByFirstKeyword(keyword);
      boolean matches;
      if (firstCandidate != null) {
        matches = applyAndRun(arguments.copy(), firstCandidate);
        if (matches) {
          return ProcessResult.SUCCESS;
        }
      }
      for (Commandlet cmd : this.commandletManager.getCommandlets()) {
        if (cmd != firstCandidate) {
          matches = applyAndRun(arguments.copy(), cmd);
          if (matches) {
            return ProcessResult.SUCCESS;
          }
        }
      }
      error("Invalid arguments: {}", current.getArgs());
    }
    this.commandletManager.getCommandlet(HelpCommandlet.class).run();
    return 1;
  }

  /**
   * @param cmd the potential {@link Commandlet} to {@link #apply(CliArguments, Commandlet, CompletionCandidateCollector) apply} and
   *        {@link Commandlet#run() run}.
   * @return {@code true} if the given {@link Commandlet} matched and did {@link Commandlet#run() run} successfully,
   *         {@code false} otherwise (the {@link Commandlet} did not match and we have to try a different candidate).
   */
  private boolean applyAndRun(CliArguments arguments, Commandlet cmd) {

    boolean matches = apply(arguments, cmd, null);
    if (matches) {
      matches = cmd.validate();
    }
    if (matches) {
      debug("Running commandlet {}", cmd);
      if (cmd.isIdeHomeRequired() && (this.ideHome == null)) {
        throw new CliException(getMessageIdeHomeNotFound());
      }
      cmd.run();
    } else {
      trace("Commandlet did not match");
    }
    return matches;
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
    CliArgument current = arguments.current();
    if (!current.isEnd()) {
      String keyword = current.get();
      Commandlet firstCandidate = this.commandletManager.getCommandletByFirstKeyword(keyword);
      boolean matches = false;
      if (firstCandidate != null) {
        matches = apply(arguments.copy(), firstCandidate, collector);
      } else if (current.isCombinedShortOption()) {
        collector.add(keyword, null, null, null);
      }
      if (!matches) {
        for (Commandlet cmd : this.commandletManager.getCommandlets()) {
          if (cmd != firstCandidate) {
            apply(arguments.copy(), cmd, collector);
          }
        }
      }
    }
    return collector.getSortedCandidates();
  }

  /**
   * @param arguments the {@link CliArguments} to apply. Will be {@link CliArguments#next() consumed} as they are
   *        matched. Consider passing a {@link CliArguments#copy() copy} as needed.
   * @param cmd the potential {@link Commandlet} to match.
   * @param collector the {@link CompletionCandidateCollector}.
   * @return {@code true} if the given {@link Commandlet} matches to the given {@link CliArgument}(s) and those have
   *         been applied (set in the {@link Commandlet} and {@link Commandlet#validate() validated}), {@code false}
   *         otherwise (the {@link Commandlet} did not match and we have to try a different candidate).
   */
  public boolean apply(CliArguments arguments, Commandlet cmd, CompletionCandidateCollector collector) {

    trace("Trying to match arguments to commandlet {}", cmd.getName());
    CliArgument currentArgument = arguments.current();
    Iterator<Property<?>> propertyIterator;
    if (currentArgument.isCompletion()) {
      propertyIterator = cmd.getProperties().iterator();
    } else {
      propertyIterator = cmd.getValues().iterator();
    }
    while (!currentArgument.isEnd()) {
      trace("Trying to match argument '{}'", currentArgument);
      Property<?> property = null;
      if (!arguments.isEndOptions()) {
        property = cmd.getOption(currentArgument.getKey());
      }
      if (property == null) {
        if (!propertyIterator.hasNext()) {
          trace("No option or next value found");
          return false;
        }
        property = propertyIterator.next();
      }
      trace("Next property candidate to match argument is {}", property);
      boolean matches = property.apply(arguments, this, cmd, collector);
      if (!matches || currentArgument.isCompletion()) {
        return false;
      }
      currentArgument = arguments.current();
    }
    return true;
  }

}
