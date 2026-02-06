package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.git.repository.RepositoryCommandlet;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.custom.CustomToolCommandlet;
import com.devonfw.tools.ide.tool.custom.CustomToolMetadata;
import com.devonfw.tools.ide.tool.extra.ExtraToolInstallation;
import com.devonfw.tools.ide.tool.extra.ExtraTools;
import com.devonfw.tools.ide.tool.extra.ExtraToolsMapper;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Abstract {@link Commandlet} base-class for both {@link UpdateCommandlet} and {@link CreateCommandlet}.
 */
public abstract class AbstractUpdateCommandlet extends Commandlet {

  private static final String MESSAGE_CODE_REPO_URL = """
      No code repository was given after '--code'.
      Further details can be found here: https://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc
      Please enter the code repository below that includes your settings folder.""";

  private static final String MESSAGE_SETTINGS_REPO_URL = """
      No settings found at {} and no SETTINGS_URL is defined.
      Further details can be found here: https://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc
      Please contact the technical lead of your project to get the SETTINGS_URL for your project to enter.
      In case you just want to test IDEasy you may simply hit return to install the default settings.""";

  /** {@link StringProperty} for the settings repository URL. */
  public final StringProperty settingsRepo;

  /** {@link FlagProperty} for skipping installation/updating of tools. */
  public final FlagProperty skipTools;

  /** {@link FlagProperty} for skipping the setup of git repositories. */
  public final FlagProperty skipRepositories;

  /** {@link FlagProperty} to force the update of the settings git repository. */
  public final FlagProperty forcePull;

  /** {@link FlagProperty} to force the installation/update of plugins. */
  public final FlagProperty forcePlugins;

  /** {@link FlagProperty} to force the setup of git repositories. */
  public final FlagProperty forceRepositories;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public AbstractUpdateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.skipTools = add(new FlagProperty("--skip-tools"));
    this.skipRepositories = add(new FlagProperty("--skip-repositories"));
    this.forcePull = add(new FlagProperty("--force-pull"));
    this.forcePlugins = add(new FlagProperty("--force-plugins"));
    this.forceRepositories = add(new FlagProperty("--force-repositories"));
    this.settingsRepo = new StringProperty("", false, "settingsRepository");
  }

  @Override
  public void run() {

    IdeStartContextImpl startContext = ((AbstractIdeContext) this.context).getStartContext();
    startContext.setForcePull(forcePull.isTrue());
    startContext.setForcePlugins(forcePlugins.isTrue());
    startContext.setForceRepositories(forceRepositories.isTrue());

    if (!this.context.isSettingsRepositorySymlinkOrJunction() || this.context.isForceMode() || forcePull.isTrue()) {
      updateSettings();
    }
    updateConf();
    reloadContext();

    updateSoftware();
    updateRepositories();
    createStartScripts();
  }

  private void reloadContext() {

    ((AbstractIdeContext) this.context).reload();
  }

  private void updateConf() {

    Path templatesFolder = this.context.getSettingsPath().resolve(IdeContext.FOLDER_TEMPLATES);
    if (!Files.exists(templatesFolder)) {
      Path legacyTemplatesFolder = this.context.getSettingsPath().resolve(IdeContext.FOLDER_LEGACY_TEMPLATES);
      if (Files.exists(legacyTemplatesFolder)) {
        templatesFolder = legacyTemplatesFolder;
      } else {
        this.context.warning("Templates folder is missing in settings repository.");
        return;
      }
    }

    Step step = this.context.newStep("Copy configuration templates", templatesFolder);
    final Path finalTemplatesFolder = templatesFolder;
    step.run(() -> setupConf(finalTemplatesFolder, this.context.getIdeHome()));
  }

  private void setupConf(Path template, Path conf) {

    List<Path> children = this.context.getFileAccess().listChildren(template, f -> true);
    for (Path child : children) {

      String basename = child.getFileName().toString();
      Path confPath = conf.resolve(basename);

      if (Files.isDirectory(child)) {
        if (!Files.isDirectory(confPath)) {
          this.context.getFileAccess().mkdirs(confPath);
        }
        setupConf(child, confPath);
      } else if (Files.isRegularFile(child)) {
        if (Files.isRegularFile(confPath)) {
          this.context.debug("Configuration {} already exists - skipping to copy from {}", confPath, child);
        } else {
          if (!basename.equals("settings.xml")) {
            this.context.info("Copying template {} to {}.", child, conf);
            this.context.getFileAccess().copy(child, conf);
          }
        }
      }
    }
  }

  /**
   * Updates the settings repository in IDE_HOME/settings by either cloning if no such repository exists or pulling if the repository exists then saves the
   * latest current commit ID in the file ".commit.id".
   */
  protected void updateSettings() {

    this.context.newStep(getStepMessage()).run(this::updateSettingsInStep);
  }

  protected String getStepMessage() {

    return "update (pull) settings repository";
  }

  private void updateSettingsInStep() {
    Path settingsPath = this.context.getSettingsPath();
    GitContext gitContext = this.context.getGitContext();
    // here we do not use pullOrClone to prevent asking a pointless question for repository URL...
    if (Files.isDirectory(settingsPath) && !this.context.getFileAccess().isEmptyDir(settingsPath)) {
      if (this.context.isForcePull() || this.context.isForceMode() || Files.isDirectory(settingsPath.resolve(GitContext.GIT_FOLDER))) {
        gitContext.pull(settingsPath);
        this.context.getGitContext().saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
      } else {
        this.context.info("Skipping git pull in settings due to code repository. Use --force-pull to enforce pulling.");
      }
    } else {
      GitUrl gitUrl = getOrAskSettingsUrl();
      checkProjectNameConvention(gitUrl.getProjectName());
      initializeRepository(gitUrl);
    }
  }

  private GitUrl getOrAskSettingsUrl() {

    String repository = this.settingsRepo.getValue();
    repository = handleDefaultRepository(repository);
    String userPromt;
    String defaultUrl;
    if (isCodeRepository()) {
      userPromt = "Code repository URL:";
      defaultUrl = null;
      this.context.info(MESSAGE_CODE_REPO_URL);
    } else {
      userPromt = "Settings URL [" + IdeContext.DEFAULT_SETTINGS_REPO_URL + "]:";
      defaultUrl = IdeContext.DEFAULT_SETTINGS_REPO_URL;
      this.context.info(MESSAGE_SETTINGS_REPO_URL, this.context.getSettingsPath());
    }
    while (repository == null || repository.isBlank()) {
      repository = this.context.askForInput(userPromt, defaultUrl);
      repository = handleDefaultRepository(repository);
    }
    GitUrl gitUrl = GitUrl.of(repository);
    while (!gitUrl.isValid()) {
      this.context.warning("The input URL is not valid, please try again:");
      repository = this.context.askForInput(userPromt, defaultUrl);
      repository = handleDefaultRepository(repository);
      gitUrl = GitUrl.of(repository);
    }
    return gitUrl;
  }

  private String handleDefaultRepository(String repository) {
    if ("-".equals(repository)) {
      if (isCodeRepository()) {
        this.context.warning("'-' is found after '--code'. This is invalid.");
        repository = null;
      } else {
        this.context.info("'-' was found for settings repository, the default settings repository '{}' will be used.", IdeContext.DEFAULT_SETTINGS_REPO_URL);
        repository = IdeContext.DEFAULT_SETTINGS_REPO_URL;
      }
    }
    return repository;
  }

  private void checkProjectNameConvention(String projectName) {
    boolean isSettingsRepo = projectName.contains(IdeContext.SETTINGS_REPOSITORY_KEYWORD);
    boolean codeRepository = isCodeRepository();
    if (isSettingsRepo == codeRepository) {
      String warningTemplate;
      if (codeRepository) {
        warningTemplate = """
            Your git URL is pointing to the project name {} that contains the keyword '{}'.
            Therefore we assume that you did a mistake by adding the '--code' option to the ide project creation.
            Do you really want to create the project?""";
      } else {
        warningTemplate = """
            Your git URL is pointing to the project name {} that does not contain the keyword ''{}''.
            Therefore we assume that you forgot to add the '--code' option to the ide project creation.
            Do you really want to create the project?""";
      }
      this.context.askToContinue(warningTemplate, projectName,
          IdeContext.SETTINGS_REPOSITORY_KEYWORD);
    }
  }

  private void initializeRepository(GitUrl gitUrl) {

    GitContext gitContext = this.context.getGitContext();
    Path settingsPath = this.context.getSettingsPath();
    Path repoPath = settingsPath;
    boolean codeRepository = isCodeRepository();
    if (codeRepository) {
      // clone the given code repository into IDE_HOME/workspaces/main
      repoPath = context.getWorkspacePath().resolve(gitUrl.getProjectName());
    }
    gitContext.pullOrClone(gitUrl, repoPath);
    if (codeRepository) {
      // check for settings folder and create symlink to IDE_HOME/settings
      Path settingsFolder = repoPath.resolve(IdeContext.FOLDER_SETTINGS);
      if (Files.exists(settingsFolder)) {
        context.getFileAccess().symlink(settingsFolder, settingsPath);
      } else {
        throw new CliException("Invalid code repository " + gitUrl + ": missing a settings folder at " + settingsFolder);
      }
    }
    this.context.getGitContext().saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
  }


  private void updateSoftware() {

    if (this.skipTools.isTrue()) {
      this.context.info("Skipping installation/update of tools as specified by the user.");
      return;
    }
    Step step = this.context.newStep("Install or update software");
    step.run(() -> doUpdateSoftwareStep(step));
  }

  private void doUpdateSoftwareStep(Step step) {

    Set<ToolCommandlet> toolCommandlets = new HashSet<>();
    CommandletManager commandletManager = this.context.getCommandletManager();
    // installed tools in IDE_HOME/software
    List<Path> softwarePaths = this.context.getFileAccess().listChildren(this.context.getSoftwarePath(), Files::isDirectory);
    for (Path softwarePath : softwarePaths) {
      String toolName = softwarePath.getFileName().toString();
      ToolCommandlet toolCommandlet = commandletManager.getToolCommandlet(toolName);
      if (toolCommandlet != null) {
        toolCommandlets.add(toolCommandlet);
      }
    }

    // regular tools in $IDE_TOOLS
    List<String> regularTools = IdeVariables.IDE_TOOLS.get(this.context);
    if (regularTools != null) {
      for (String regularTool : regularTools) {
        ToolCommandlet toolCommandlet = commandletManager.getToolCommandlet(regularTool);
        if (toolCommandlet == null) {
          String displayName = (regularTool == null || regularTool.isBlank()) ? "<empty>" : "'" + regularTool + "'";
          this.context.error("Cannot install or update tool '{}''. No matching commandlet found. Please check your IDE_TOOLS configuration.", displayName);
        } else {
          toolCommandlets.add(toolCommandlet);
        }
      }
    }

    // custom tools in ide-custom-tools.json
    for (CustomToolMetadata customTool : this.context.getCustomToolRepository().getTools()) {
      CustomToolCommandlet customToolCommandlet = new CustomToolCommandlet(this.context, customTool);
      toolCommandlets.add(customToolCommandlet);
    }

    // update/install the toolCommandlets
    for (ToolCommandlet toolCommandlet : toolCommandlets) {
      this.context.newStep("Install " + toolCommandlet.getName()).run(() -> toolCommandlet.install(false));
    }

    ExtraTools extraTools = ExtraToolsMapper.get().loadJsonFromFolder(this.context.getSettingsPath());
    if (extraTools != null) {
      List<String> toolNames = extraTools.getSortedToolNames();
      this.context.info("Found extra installation of the following tools: {}", toolNames);
      for (String tool : toolNames) {
        List<ExtraToolInstallation> installations = extraTools.getExtraInstallations(tool);
        this.context.newStep("Install extra version(s) of " + tool).run(() -> installExtraToolInstallations(tool, installations));
      }
    }
  }

  private void installExtraToolInstallations(String tool, List<ExtraToolInstallation> extraInstallations) {

    CommandletManager commandletManager = this.context.getCommandletManager();
    FileAccess fileAccess = this.context.getFileAccess();
    Path extraPath = this.context.getSoftwareExtraPath();
    LocalToolCommandlet toolCommandlet = commandletManager.getRequiredLocalToolCommandlet(tool);
    for (ExtraToolInstallation extraInstallation : extraInstallations) {
      ToolInstallRequest request = new ToolInstallRequest(false);
      String edition = extraInstallation.edition();
      if (edition == null) {
        edition = toolCommandlet.getConfiguredEdition();
      }
      ToolEdition toolEdition = new ToolEdition(tool, edition);
      VersionIdentifier version = extraInstallation.version();
      request.setRequested(new ToolEditionAndVersion(toolEdition, version));
      Path extraToolPath = extraPath.resolve(tool);
      Path toolPath = extraToolPath.resolve(extraInstallation.name());
      request.setToolPathForExtraInstallation(toolPath);
      toolCommandlet.install(request);
    }
  }

  private void updateRepositories() {

    if (this.skipRepositories.isTrue()) {
      if (this.forceRepositories.isTrue()) {
        this.context.warning("Options to skip and force repositories are incompatible and should not be combined. Ignoring --force-repositories to proceed.");
      }
      this.context.info("Skipping setup of repositories as specified by the user.");
      return;
    }
    RepositoryCommandlet repositoryCommandlet = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    repositoryCommandlet.reset();
    repositoryCommandlet.run();
  }

  private void createStartScripts() {

    List<String> ides = IdeVariables.CREATE_START_SCRIPTS.get(this.context);
    if (ides == null) {
      this.context.info("Variable CREATE_START_SCRIPTS is undefined - skipping start script creation.");
      return;
    }
    for (String ide : ides) {
      ToolCommandlet tool = this.context.getCommandletManager().getToolCommandlet(ide);
      if (tool == null) {
        this.context.error("Undefined IDE '{}' configured in variable CREATE_START_SCRIPTS.");
      } else {
        createStartScript(ide);
      }
    }
  }

  private void createStartScript(String ide) {

    this.context.info("Creating start scripts for {}", ide);
    Path workspaces = this.context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES);
    try (Stream<Path> childStream = Files.list(workspaces)) {
      Iterator<Path> iterator = childStream.iterator();
      while (iterator.hasNext()) {
        Path child = iterator.next();
        if (Files.isDirectory(child)) {
          createStartScript(ide, child.getFileName().toString());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to list children of directory " + workspaces, e);
    }
  }

  private void createStartScript(String ide, String workspace) {

    Path ideHome = this.context.getIdeHome();
    String scriptName = ide + "-" + workspace;
    boolean windows = this.context.getSystemInfo().isWindows();
    if (windows) {
      scriptName = scriptName + ".bat";
    } else {
      scriptName = scriptName + ".sh";
    }
    Path scriptPath = ideHome.resolve(scriptName);
    if (Files.exists(scriptPath)) {
      return;
    }
    String scriptContent;
    if (windows) {
      scriptContent = "@echo off\r\n"
          + "pushd %~dp0\r\n"
          + "cd workspaces/" + workspace + "\r\n"
          + "call ide " + ide + "\r\n"
          + "popd\r\n";
    } else {
      scriptContent = "#!/usr/bin/env bash\n"
          + "cd \"$(dirname \"$0\")\"\n"
          + "cd workspaces/" + workspace + "\n"
          + "ideasy " + ide + "\n";
    }
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.writeFileContent(scriptContent, scriptPath);
    fileAccess.makeExecutable(scriptPath);
  }

  /**
   * Judge if the repository is a code repository.
   *
   * @return true when the repository is a code repository, otherwise false.
   */
  protected boolean isCodeRepository() {
    return false;
  }

}
