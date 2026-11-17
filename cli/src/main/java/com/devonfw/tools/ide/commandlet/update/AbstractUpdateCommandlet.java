package com.devonfw.tools.ide.commandlet.update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliFatalException;
import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.commandlet.CreateCommandlet;
import com.devonfw.tools.ide.commandlet.update.settings.SettingsHealthCheckResult;
import com.devonfw.tools.ide.commandlet.update.settings.SettingsUpdateResult;
import com.devonfw.tools.ide.commandlet.update.settings.SettingsUpdateStatus;
import com.devonfw.tools.ide.commandlet.update.settings.SettingsUpdater;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.git.repository.RepositoryCommandlet;
import com.devonfw.tools.ide.git.repository.RepositoryType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallTracker;
import com.devonfw.tools.ide.tool.custom.CustomToolCommandlet;
import com.devonfw.tools.ide.tool.custom.CustomToolMetadata;
import com.devonfw.tools.ide.tool.extra.ExtraToolInstallation;
import com.devonfw.tools.ide.tool.extra.ExtraTools;
import com.devonfw.tools.ide.tool.extra.ExtraToolsMapper;
import com.devonfw.tools.ide.util.FilenameUtil;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Abstract {@link AbstractCommandlet} base-class for both {@link UpdateCommandlet} and {@link CreateCommandlet}.
 */
public abstract class AbstractUpdateCommandlet extends AbstractCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractUpdateCommandlet.class);

  private static final Set<String> TEXTUAL_EXTENSIONS = Set.of("txt", "adoc", "xml", "json", "md", "asciidoc", "yaml", "yml", "properties");

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

    updateSettings();
    updateProjectTemplates();
    reloadContext();
    this.context.verifyIdeMinVersion(true);

    updateSoftware();
    updateRepositories();
    createStartScripts();
  }

  /**
   * Hook that is called after the settings passed the health check but before they are moved to their final location. Does nothing by default and is overridden
   * by {@link CreateCommandlet} to create the project structure so that no project is created at all if the health check failed.
   */
  protected void onSettingHealthCheckFinished() {

    // nothing to do by default
  }

  private void reloadContext() {

    ((AbstractIdeContext) this.context).reload();
  }

  private void updateProjectTemplates() {

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
    step.run(() -> setupProjectTemplates(finalTemplatesFolder, this.context.getIdeHome()));
  }

  private void setupProjectTemplates(Path templatePath, Path projectPath) {

    EnvironmentVariables variables = this.context.getVariables();
    List<Path> templateChildren = this.context.getFileAccess().listChildren(templatePath, f -> true);
    for (Path templateChild : templateChildren) {

      String basename = templateChild.getFileName().toString();
      Path projectChild = projectPath.resolve(basename);

      if (Files.isDirectory(templateChild)) {
        if (!Files.isDirectory(projectChild)) {
          this.context.getFileAccess().mkdirs(projectChild);
        }
        setupProjectTemplates(templateChild, projectChild);
      } else if (Files.isRegularFile(templateChild)) {
        if (Files.isRegularFile(projectChild)) {
          LOG.debug("Configuration {} already exists - skipping to instantiate from {}", projectChild, templateChild);
        } else {
          if (basename.equals("settings.xml")) {
            LOG.debug("Skipping settings.xml that will later be instantiated via mvn.");
          } else {
            this.context.newStep("Instantiate template", templateChild, projectChild).run(() ->
                setupProjectTemplate(templateChild, projectChild, variables));
          }
        }
      }
    }
  }

  private void setupProjectTemplate(Path templateFile, Path projectFile, EnvironmentVariables variables) {
    String basename = templateFile.getFileName().toString();
    String extension = FilenameUtil.getExtension(basename);
    if (TEXTUAL_EXTENSIONS.contains(extension)) {
      instantiateProjectTemplate(templateFile, projectFile, variables);
    } else {
      this.context.getFileAccess().copy(templateFile, projectFile.getParent());
    }
  }

  private void instantiateProjectTemplate(Path templateFile, Path projectFile, EnvironmentVariables variables) {

    try (BufferedReader reader = Files.newBufferedReader(templateFile);
        BufferedWriter writer = Files.newBufferedWriter(projectFile)
    ) {
      String line;
      do {
        line = reader.readLine();
        if (line != null) {
          String resolvedLine = variables.resolve(line, templateFile, false);
          writer.write(resolvedLine);
          writer.write('\n');
        }
      } while (line != null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to instantiate template ", e);
    }
  }


  /**
   * Updates the settings repository in IDE_HOME/settings by either cloning if no such repository exists or pulling if the repository exists then saves the
   * latest current commit ID in the file ".commit.id". The settings are always cloned into a temporary directory first where a health check is performed. Only
   * if that health check succeeded the settings are pulled or the verified clone is moved to its final location.
   */
  protected void updateSettings() {

    RepositoryType repositoryType = RepositoryType.ofSettingsPath(getSettingsPathForSettingsUpdate(), context);
    if (repositoryType == RepositoryType.CODE_SETTINGS_COMBINED && !(this.context.isForceMode() || this.forcePull.isTrue())) {
      LOG.info("Skipping git pull in settings due to code repository. Use --force-pull to enforce pulling.");
      return;
    }
    Step step = this.context.newStep(getStepMessage());
    step.run(this::updateSettingsInStep);
  }

  protected String getStepMessage() {

    return "Update settings repository";
  }

  private void updateSettingsInStep() {

    SettingsUpdater settingsUpdater = new SettingsUpdater(this.context, this.settingsRepo);
    try {
      //Step 1: Perform health check
      Step healthCheckStep = this.context.newStep("Performing settings health check");
      SettingsHealthCheckResult healthCheckResult = healthCheckStep.call(() -> checkSettingsInStep(settingsUpdater), () -> null);

      // If the health check failed (healthCheckResult is null) the settings have not been verified, so skip applying them and fail the "Update settings"
      // step. A non-null result is only produced when the health check passed or the user explicitly chose to continue anyway (force mode), so this never
      // aborts in force mode.
      if (healthCheckResult == null) {
        throw new CliException("Settings update aborted due to error in health check");
      }

      //Step 2: Let create/update commandlets prepare themselves for the settings update.
      onSettingHealthCheckFinished();

      //Step 3: Apply (move/pull newest version) settings
      Step applySettingsStep = this.context.newStep("Applying settings");
      applySettingsStep.run(() -> applySettingsUpdateInStep(settingsUpdater, healthCheckResult));

      //Make sure to always fail the parent step if the "Apply settings" step fails.
      if (applySettingsStep.isFailure()) {
        throw new CliException("Settings update failed due to error while applying the settings update");
      }
    } finally {
      // the verified clone lives across both steps and the prepareProject hook so it is only here that its lifetime ends
      settingsUpdater.cleanup();
    }
  }

  private SettingsHealthCheckResult checkSettingsInStep(SettingsUpdater settingsUpdater) {
    SettingsHealthCheckResult healthCheckResult = settingsUpdater.checkSettings(getSettingsPathForSettingsUpdate());
    RepositoryType repositoryType = healthCheckResult.repositoryType();

    if ((repositoryType == null || !repositoryType.isValid()) && !healthCheckResult.isExistingProject()) {
      throw new CliFatalException("Fatal error while cloning settings: The settings health check failed: " + healthCheckResult.errorMessage());
    } else if ((repositoryType == null || !repositoryType.isValid())) {
      throw new CliException("The settings health check failed: " + healthCheckResult.errorMessage());
    }
    return healthCheckResult;
  }

  private void applySettingsUpdateInStep(SettingsUpdater settingsUpdater, SettingsHealthCheckResult healthCheckResult) {
    SettingsUpdateResult settingsUpdateResult = settingsUpdater.applySettings(healthCheckResult);
    if (settingsUpdateResult == null) {

      throw new CliException("Failed to apply the settings update due to unknown error.");
    }

    if (Objects.requireNonNull(settingsUpdateResult.updateStatus()) == SettingsUpdateStatus.SETTINGS_UPDATE_FAILED) {
      String errorMessage = "The settings update could not be applied: " + settingsUpdateResult.errorMessage();
      if (!healthCheckResult.isExistingProject()) {
        throw new CliFatalException(errorMessage);
      }
      throw new CliException(errorMessage);
    }
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

    Set<AbstractToolCommandlet> toolCommandlets = new HashSet<>();
    CommandletManager commandletManager = this.context.getCommandletManager();
    // installed tools in IDE_HOME/software
    List<Path> softwarePaths = this.context.getFileAccess().listChildren(this.context.getSoftwarePath(), Files::isDirectory);
    for (Path softwarePath : softwarePaths) {
      String toolName = softwarePath.getFileName().toString();
      AbstractToolCommandlet toolCommandlet = commandletManager.getToolCommandlet(toolName);
      if (toolCommandlet != null) {
        toolCommandlets.add(toolCommandlet);
      }
    }

    // regular tools in $IDE_TOOLS
    List<String> regularTools = IdeVariables.IDE_TOOLS.get(this.context);
    if (regularTools != null) {
      for (String regularTool : regularTools) {
        AbstractToolCommandlet toolCommandlet = commandletManager.getToolCommandlet(regularTool);
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

    // Track tools installed during this operation so a shared dependency is not installed and CVE-checked redundantly. A single tracker is shared across
    // all tools so cross-tool references are deduplicated too - see https://github.com/devonfw/IDEasy/issues/945
    ToolInstallTracker tracker = new ToolInstallTracker();

    // update/install the toolCommandlets
    for (AbstractToolCommandlet toolCommandlet : toolCommandlets) {
      this.context.newStep("Install " + toolCommandlet.getName()).run(() -> toolCommandlet.install(tracker));
    }

    ExtraTools extraTools = ExtraToolsMapper.get().loadJsonFromFolder(this.context.getSettingsPath());
    if (extraTools != null) {
      List<String> toolNames = extraTools.getSortedToolNames();
      LOG.info("Found extra installation of the following tools: {}", toolNames);
      for (String tool : toolNames) {
        List<ExtraToolInstallation> installations = extraTools.getExtraInstallations(tool);
        this.context.newStep("Install extra version(s) of " + tool).run(() -> installExtraToolInstallations(tool, installations, tracker));
      }
    }

  }

  private void installExtraToolInstallations(String tool, List<ExtraToolInstallation> extraInstallations, ToolInstallTracker tracker) {

    CommandletManager commandletManager = this.context.getCommandletManager();
    FileAccess fileAccess = this.context.getFileAccess();
    Path extraPath = this.context.getSoftwareExtraPath();
    AbstractLocalToolCommandlet toolCommandlet = commandletManager.getRequiredLocalToolCommandlet(tool);
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
      request.setTracker(tracker);
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
      AbstractToolCommandlet tool = this.context.getCommandletManager().getToolCommandlet(ide);
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

  /**
   * This method returns the path to the settings for the case of a settings update.
   *
   * @return The {@link Path} to the settings folder; if not overridden we will get the path from the context
   */
  protected Path getSettingsPathForSettingsUpdate() {

    return this.context.getSettingsPath();
  }
}
