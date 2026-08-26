package com.devonfw.tools.ide.commandlet.update;

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

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.commandlet.CreateCommandlet;
import com.devonfw.tools.ide.commandlet.update.SettingsUpdater.ResultStatus;
import com.devonfw.tools.ide.commandlet.update.SettingsUpdater.SettingsUpdateResult;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
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

  private static final Logger LOG = LoggerFactory.getLogger(AbstractUpdateCommandlet.class);

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
    updateConf();
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
  protected void prepareProject() {

    // nothing to do by default
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
   * latest current commit ID in the file ".commit.id". The settings are always cloned into a temporary directory first where a health check is performed. Only
   * if that health check succeeded the settings are pulled or the verified clone is moved to its final location.
   */
  protected void updateSettings() {

    boolean codeRepository = this.context.isSettingsCodeRepository();
    if (codeRepository && !(this.context.isForceMode() || this.forcePull.isTrue())) {
      LOG.info("Skipping git pull in settings due to code repository. Use --force-pull to enforce pulling.");
      return;
    }
    Step step = this.context.newStep(getStepMessage());
    step.run(() -> updateSettingsInStep(step));
  }

  protected String getStepMessage() {

    return "Update settings repository";
  }

  private void updateSettingsInStep(Step step) {

    SettingsUpdater settingsUpdater = new SettingsUpdater(this.context, this.settingsRepo);
    try {
      SettingsUpdateResult result = this.context.newStep("Performing settings health check").call(settingsUpdater::checkSettings, () -> null);

      // fatal problems (e.g. no valid settings at all) were already rethrown, so reaching this point means the settings we have stay usable
      if (result == null) {
        step.error("Health check on settings failed due to unknown error - the settings have not been updated.");
        return;
      } else if (result.status() == ResultStatus.SETTINGS_UPDATE_FAILED) {
        step.error("The settings have not been updated: {}", result.errorMessage());
        return;
      }
      prepareProject();
      boolean applied = this.context.newStep("Applying settings").run(() -> settingsUpdater.applySettings(result));
      if (!applied) {
        step.error("Failed to apply the settings update.");
      }
    } finally {
      // the verified clone lives across both steps and the prepareProject hook so it is only here that its lifetime ends
      settingsUpdater.cleanup();
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
