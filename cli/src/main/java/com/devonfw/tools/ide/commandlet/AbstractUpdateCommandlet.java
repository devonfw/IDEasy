package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.git.repository.RepositoryCommandlet;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
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

  private static final Logger LOG = LoggerFactory.getLogger(AbstractUpdateCommandlet.class);

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
  protected void doRun() {

    IdeStartContextImpl startContext = ((AbstractIdeContext) this.context).getStartContext();
    startContext.setForcePull(forcePull.isTrue());
    startContext.setForcePlugins(forcePlugins.isTrue());
    startContext.setForceRepositories(forceRepositories.isTrue());

    if (!this.context.isSettingsRepositorySymlinkOrJunction() || this.context.isForceMode() || forcePull.isTrue()) {
      updateSettings();
      // Check if instance of create commandlet. Only then will we analyze the project
      if (this instanceof CreateCommandlet) {
        analyze_project();
      }
    }
    updateConf();
    reloadContext();

    updateSoftware();
    updateRepositories();
    createStartScripts();
  }

  /**
   * This method is invoked when a new porject is created. It analyzes the cloned repository to check if it is a valid IDEasy repository. The repository can either be a settings repository (with ide.properties or devon.properties on the top level)
   * or a code repository (with a settings folder on the top level containing such a file). Otherwise, the project creatio fails and an error message is logged.
   */
  private void analyze_project() {
    // Settings repository: ide.properties on top levels (or devon.properties for legacy users)
    // Code repository: settings folder on top level with ide.properties inside (or devon.properties for legacy users)
    String projectName = this.context.getProjectName();
    Path actualProjectPath = null;

    //Check if a file called ide.properties or devon.properties in settingsPath
    Path SettingsPath = this.context.getSettingsPath();
    if (Files.exists(SettingsPath.resolve("ide.properties")) || Files.exists(SettingsPath.resolve("devon.properties"))) {
      // Repository is a settings repository: ide.properties on top levels (or devon.properties for legacy users)
      LOG.info("The repository seems to be a settings repository based on the presence of ide.properties or devon.properties on the top level.");

      actualProjectPath = this.context.getIdeRoot();
      moveProject(this.context.getIdeHome(), actualProjectPath);
    } else if (Files.exists(SettingsPath.resolve("settings/ide.properties")) || Files.exists(SettingsPath.resolve("settings/devon.properties"))) {
      // Repository is a code repository: settings folder on top level with ide.properties inside (or devon.properties for legacy users)
      LOG.info("ide.properties or devon.properties (legacy) found in settings subfolder. This indicates a code repository with settings folder on the top level.");

      // Move settings folder contents containing code in workspace/main
      actualProjectPath = this.context.getIdeRoot().resolve(projectName).resolve("workspaces/main/").resolve(projectName);
      for (Path child : this.context.getFileAccess().listChildren(SettingsPath, f -> true)) {
        System.out.println("Child: " + child);
        moveProject(child, actualProjectPath);
      }
      // Move remaining folders into IDE_HOME
      actualProjectPath = this.context.getIdeRoot();
      moveProject(this.context.getIdeHome(), actualProjectPath);
      // Delete empty settings folder in IDE_ROOT/<project_name> so we can create a symlink in the next step
      this.context.getFileAccess().delete(actualProjectPath.resolve(projectName).resolve("settings"));

      // Link settings folder in IDE_HOME to settings folder in code repository
      this.context.getFileAccess().symlink(actualProjectPath.resolve(projectName).resolve("workspaces/main").resolve(projectName).resolve("settings"), actualProjectPath.resolve(projectName).resolve("settings"));

      // Final cleanup in temp location
      this.context.getFileAccess().delete(this.context.getIdeHome());
    } else {
      // Repository seems to be invalid. Clean up temporary location and return error
      this.context.getFileAccess().delete(this.context.getIdeHome());
      throw new CliException("This repository does not include an ide.properties file at the top level or a settings folder with such a file. "
      + "The respository does not seem to be a valid IDEasy repository. Please verify the repository and try again.");
    }
    // Set IDE_HOME to new (and actual) project location
    this.context.setIdeHome(this.context.getIdeRoot().resolve(projectName));
  }

  /**
   * Moves files of a new projectfrom the temporary location to the final project location.
   * @param oldPath - The path of the file or directory to be moved.
   * @param newPath - The path of the destination.
   */
  private void moveProject(Path oldPath, Path newPath) {
    try {
      this.context.getFileAccess().copy(oldPath, newPath, FileCopyMode.COPY_TREE_OVERRIDE_FILES);
      this.context.getFileAccess().delete(oldPath);
    } catch (Exception e) {
      LOG.error("Failed to move project from {} to {}. Please move it manually.", oldPath, newPath, e);
    }
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
        LOG.warn("Templates folder is missing in settings repository.");
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
          LOG.debug("Configuration {} already exists - skipping to copy from {}", confPath, child);
        } else {
          if (!basename.equals("settings.xml")) {
            LOG.info("Copying template {} to {}.", child, conf);
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
    if (Files.isDirectory(settingsPath) && this.context.getGitContext().isGitRepo(settingsPath) || this.context.isSettingsRepositorySymlinkOrJunction()) {
      if (this.context.isForcePull() || this.context.isForceMode()) {
        if (gitContext.hasUntrackedFiles(settingsPath)) {
          gitContext.pullSafelyWithStash(settingsPath);
        } else {
          gitContext.pull(settingsPath);
        }
        this.context.getGitContext().saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
      } else {
        LOG.info("Skipping git pull in settings due to code repository. Use --force-pull to enforce pulling.");
      }
    } else {
      if (!this.context.getFileAccess().isEmptyDir(settingsPath)) {
        this.context.askToContinue(
          "Your settings repository seems to be broken ('.git' folder not present). We can fix this by moving "
          + " your settings the backed up. You will be asked for the settings git URL and your settings will be cloned from scratch. Do you want to proceed?"
        );

        this.context.getFileAccess().backup(settingsPath);
      }

      GitUrl gitUrl = getOrAskSettingsUrl();
      initializeRepository(gitUrl);
    }
  }

  private GitUrl getOrAskSettingsUrl() {

    String repository = this.settingsRepo.getValue();
    repository = handleDefaultRepository(repository);
    String userPromt = "Repository URL [" + IdeContext.DEFAULT_SETTINGS_REPO_URL + "]:";
    String defaultUrl = IdeContext.DEFAULT_SETTINGS_REPO_URL;
    LOG.info(MESSAGE_SETTINGS_REPO_URL, this.context.getSettingsPath());

    GitUrl gitUrl = null;
    if (repository != null) {
      gitUrl = GitUrl.of(repository);
    }
    while ((gitUrl == null) || !gitUrl.isValid()) {
      repository = this.context.askForInput(userPromt, defaultUrl);
      repository = handleDefaultRepository(repository);
      gitUrl = GitUrl.of(repository);
      if (!gitUrl.isValid()) {
        LOG.warn("The input URL is not valid, please try again.");
      }
    }
    return gitUrl;
  }

  private String handleDefaultRepository(String repository) {
    if ("-".equals(repository)) {
      LOG.info("'-' was found for the repository, the default settings repository '{}' will be used.", IdeContext.DEFAULT_SETTINGS_REPO_URL);
      repository = IdeContext.DEFAULT_SETTINGS_REPO_URL;
    }
    return repository;
  }

  private void initializeRepository(GitUrl gitUrl) {

    GitContext gitContext = this.context.getGitContext();
    Path settingsPath = this.context.getSettingsPath();
    Path repoPath = settingsPath;
    gitContext.pullOrClone(gitUrl, repoPath);
    this.context.getGitContext().saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
  }

  private void updateSoftware() {

    if (this.skipTools.isTrue()) {
      LOG.info("Skipping installation/update of tools as specified by the user.");
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
          LOG.error("Cannot install or update tool '{}''. No matching commandlet found. Please check your IDE_TOOLS configuration.", displayName);
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
      LOG.info("Found extra installation of the following tools: {}", toolNames);
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
        LOG.warn("Options to skip and force repositories are incompatible and should not be combined. Ignoring --force-repositories to proceed.");
      }
      LOG.info("Skipping setup of repositories as specified by the user.");
      return;
    }
    RepositoryCommandlet repositoryCommandlet = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    repositoryCommandlet.reset();
    repositoryCommandlet.run();
  }

  private void createStartScripts() {

    List<String> ides = IdeVariables.CREATE_START_SCRIPTS.get(this.context);
    if (ides == null) {
      LOG.info("Variable CREATE_START_SCRIPTS is undefined - skipping start script creation.");
      return;
    }
    for (String ide : ides) {
      ToolCommandlet tool = this.context.getCommandletManager().getToolCommandlet(ide);
      if (tool == null) {
        LOG.error("Undefined IDE '{}' configured in variable CREATE_START_SCRIPTS.", ide);
      } else {
        createStartScript(ide);
      }
    }
  }

  private void createStartScript(String ide) {

    LOG.info("Creating start scripts for {}", ide);
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
      scriptContent = "@echo off\r\n" + "pushd %~dp0\r\n" + "cd workspaces/" + workspace + "\r\n" + "call ide " + ide + "\r\n" + "popd\r\n";
    } else {
      scriptContent = "#!/usr/bin/env bash\n" + "cd \"$(dirname \"$0\")\"\n" + "cd workspaces/" + workspace + "\n" + "ideasy " + ide + "\n";
    }
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.writeFileContent(scriptContent, scriptPath);
    fileAccess.makeExecutable(scriptPath);
  }
}
