package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.cli.CliAbortException;
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
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.repo.CustomToolRepository;
import com.devonfw.tools.ide.repo.CustomToolRepositoryImpl;
import com.devonfw.tools.ide.repo.DefaultToolRepository;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.step.StepImpl;
import com.devonfw.tools.ide.url.model.UrlMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Abstract base implementation of {@link IdeContext}.
 */
public abstract class AbstractIdeContext implements IdeContext {

  private static final String IDE_URLS_GIT = "https://github.com/devonfw/ide-urls.git";

  private final Map<IdeLogLevel, IdeSubLogger> loggers;

  private Path ideHome;

  private final Path ideRoot;

  private Path confPath;

  private Path settingsPath;

  private Path softwarePath;

  private Path softwareExtraPath;

  private Path softwareRepositoryPath;

  private Path pluginsPath;

  private Path workspacePath;

  private String workspaceName;

  private Path urlsPath;

  private Path tempPath;

  private Path tempDownloadPath;

  private Path cwd;

  private Path downloadPath;

  private Path toolRepository;

  private Path userHome;

  private Path userHomeIde;

  private SystemPath path;

  private final SystemInfo systemInfo;

  private EnvironmentVariables variables;

  private final FileAccess fileAccess;

  private final CommandletManager commandletManager;

  private final ToolRepository defaultToolRepository;

  private CustomToolRepository customToolRepository;

  private DirectoryMerger workspaceMerger;

  private final Function<IdeLogLevel, IdeSubLogger> loggerFactory;

  private boolean offlineMode;

  private boolean forceMode;

  private boolean batchMode;

  private boolean quietMode;

  private Locale locale;

  private UrlMetadata urlMetadata;

  private Path defaultExecutionDirectory;

  private StepImpl currentStep;

  /**
   * The constructor.
   *
   * @param minLogLevel the minimum {@link IdeLogLevel} to enable. Should be {@link IdeLogLevel#INFO} by default.
   * @param factory the {@link Function} to create {@link IdeSubLogger} per {@link IdeLogLevel}.
   * @param userDir the optional {@link Path} to current working directory.
   * @param toolRepository @param toolRepository the {@link ToolRepository} of the context. If it is set to {@code null} {@link DefaultToolRepository} will be
   * used.
   */
  public AbstractIdeContext(IdeLogLevel minLogLevel, Function<IdeLogLevel, IdeSubLogger> factory, Path userDir, ToolRepository toolRepository) {

    super();
    this.loggerFactory = factory;
    this.loggers = new HashMap<>();
    setLogLevel(minLogLevel);
    this.systemInfo = SystemInfoImpl.INSTANCE;
    this.commandletManager = new CommandletManagerImpl(this);
    this.fileAccess = new FileAccessImpl(this);
    String workspace = WORKSPACE_MAIN;
    if (userDir == null) {
      userDir = Path.of(System.getProperty("user.dir"));
    } else {
      userDir = userDir.toAbsolutePath();
    }
    // detect IDE_HOME and WORKSPACE
    Path currentDir = userDir;
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
    setCwd(userDir, workspace, currentDir);
    Path ideRootPath = null;
    if (currentDir == null) {
      info(getMessageIdeHomeNotFound());
    } else {
      debug(getMessageIdeHomeFound());
      ideRootPath = this.ideHome.getParent();
    }

    if (!isTest()) {
      String root = System.getenv("IDE_ROOT");
      if (root != null) {
        Path rootPath = Path.of(root);
        if (ideRootPath == null) {
          ideRootPath = rootPath;
        } else if (!ideRootPath.equals(rootPath)) {
          warning("Variable IDE_ROOT is set to '{}' but for your project '{}' the path '{}' would have been expected.", rootPath, this.ideHome.getFileName(),
              ideRootPath);
        }
      }
    }
    if (ideRootPath == null || !Files.isDirectory(ideRootPath)) {
      error("IDE_ROOT is not set or not a valid directory.");
    }
    this.ideRoot = ideRootPath;

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

    if (toolRepository == null) {
      this.defaultToolRepository = new DefaultToolRepository(this);
    } else {
      this.defaultToolRepository = toolRepository;
    }
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
      this.softwarePath = null;
      this.softwareExtraPath = null;
      this.pluginsPath = null;
    } else {
      this.workspacePath = this.ideHome.resolve(FOLDER_WORKSPACES).resolve(this.workspaceName);
      this.confPath = this.ideHome.resolve(FOLDER_CONF);
      this.settingsPath = this.ideHome.resolve(FOLDER_SETTINGS);
      this.softwarePath = this.ideHome.resolve(FOLDER_SOFTWARE);
      this.softwareExtraPath = this.softwarePath.resolve(FOLDER_EXTRA);
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
      this.userHome = Path.of(System.getProperty("user.home"));
    }
    this.userHomeIde = this.userHome.resolve(".ide");
    this.downloadPath = this.userHome.resolve("Downloads/ide");

    this.variables = createVariables();
    this.path = computeSystemPath();
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
   * @return the status message about the {@link #getIdeHome() IDE_HOME} detection and environment variable initialization.
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

    return isMock();
  }

  /**
   * @return {@code true} if this is a mock context for JUnits, {@code false} otherwise.
   */
  public boolean isMock() {

    return false;
  }

  private SystemPath computeSystemPath() {

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
    AbstractEnvironmentVariables workspace = extendVariables(settings, this.workspacePath, EnvironmentVariablesType.WORKSPACE);
    AbstractEnvironmentVariables conf = extendVariables(workspace, this.confPath, EnvironmentVariablesType.CONF);
    return conf.resolved();
  }

  private AbstractEnvironmentVariables extendVariables(AbstractEnvironmentVariables envVariables, Path propertiesPath, EnvironmentVariablesType type) {

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
  public String getProjectName() {

    if (this.ideHome != null) {
      return this.ideHome.getFileName().toString();
    }
    return "";
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
  public Path getSoftwareExtraPath() {

    return this.softwareExtraPath;
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
        getGitContext().pullOrCloneAndResetIfNeeded(IDE_URLS_GIT, this.urlsPath, null);
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
    } catch (Exception ignored) {

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

  /**
   * @return the {@link #getDefaultExecutionDirectory() default execution directory} in which a command process is executed.
   */
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

  /**
   * @return a new instance of {@link ProcessContext}.
   * @see #newProcess()
   */
  protected ProcessContext createProcessContext() {

    return new ProcessContextImpl(this);
  }

  @Override
  public IdeSubLogger level(IdeLogLevel level) {

    IdeSubLogger logger = this.loggers.get(level);
    Objects.requireNonNull(logger);
    return logger;
  }

  @Override
  public String askForInput(String message, String defaultValue) {

    if (!message.isBlank()) {
      info(message);
    }
    if (isBatchMode()) {
      if (isForceMode()) {
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
    try {
      if (!current.isEnd()) {
        String keyword = current.get();
        Commandlet firstCandidate = this.commandletManager.getCommandletByFirstKeyword(keyword);
        boolean matches;
        if (firstCandidate != null) {
          matches = applyAndRun(arguments.copy(), firstCandidate);
          if (matches) {
            supressStepSuccess = firstCandidate.isSuppressStepSuccess();
            step.success();
            return ProcessResult.SUCCESS;
          }
        }
        for (Commandlet cmd : this.commandletManager.getCommandlets()) {
          if (cmd != firstCandidate) {
            matches = applyAndRun(arguments.copy(), cmd);
            if (matches) {
              supressStepSuccess = cmd.isSuppressStepSuccess();
              step.success();
              return ProcessResult.SUCCESS;
            }
          }
        }
        step.error("Invalid arguments: {}", current.getArgs());
      }
      this.commandletManager.getCommandlet(HelpCommandlet.class).run();
      return 1;
    } catch (Throwable t) {
      step.error(t, true);
      throw t;
    } finally {
      step.end();
      assert (this.currentStep == null);
      step.logSummary(supressStepSuccess);
    }
  }

  /**
   * @param cmd the potential {@link Commandlet} to {@link #apply(CliArguments, Commandlet, CompletionCandidateCollector) apply} and
   * {@link Commandlet#run() run}.
   * @return {@code true} if the given {@link Commandlet} matched and did {@link Commandlet#run() run} successfully, {@code false} otherwise (the
   * {@link Commandlet} did not match and we have to try a different candidate).
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
   * @param arguments the {@link CliArguments} to apply. Will be {@link CliArguments#next() consumed} as they are matched. Consider passing a
   * {@link CliArguments#copy() copy} as needed.
   * @param cmd the potential {@link Commandlet} to match.
   * @param collector the {@link CompletionCandidateCollector}.
   * @return {@code true} if the given {@link Commandlet} matches to the given {@link CliArgument}(s) and those have been applied (set in the {@link Commandlet}
   * and {@link Commandlet#validate() validated}), {@code false} otherwise (the {@link Commandlet} did not match and we have to try a different candidate).
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
    throw new IllegalStateException("Could not find Bash. Please install Git for Windows and rerun.");
  }

}
