package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.git.repository.RepositoryCommandlet;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.CustomToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.repository.CustomToolMetadata;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Abstract {@link Commandlet} base-class for both {@link UpdateCommandlet} and {@link CreateCommandlet}.
 */
public abstract class AbstractUpdateCommandlet extends Commandlet {

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

    this.context.setForcePull(forcePull.isTrue());
    this.context.setForcePlugins(forcePlugins.isTrue());
    this.context.setForceRepositories(forceRepositories.isTrue());

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

    try (Step step = this.context.newStep("Copy configuration templates", templatesFolder)) {
      setupConf(templatesFolder, this.context.getIdeHome());
      step.success();
    }
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

    Path settingsPath = this.context.getSettingsPath();
    GitContext gitContext = this.context.getGitContext();
    Step step = null;
    try {
      // here we do not use pullOrClone to prevent asking a pointless question for repository URL...
      if (Files.isDirectory(settingsPath) && !this.context.getFileAccess().isEmptyDir(settingsPath)) {
        step = this.context.newStep("Pull settings repository");
        gitContext.pull(settingsPath);
      } else {
        step = this.context.newStep("Clone settings repository");
        // check if a settings repository is given, otherwise prompt user for a repository.
        String repository = this.settingsRepo.getValue();
        if (repository == null) {
          String message = "Missing your settings at " + settingsPath + " and no SETTINGS_URL is defined.\n"
              + "Further details can be found here: https://github.com/devonfw/IDEasy/blob/main/documentation/settings.asciidoc\n"
              + "Please contact the technical lead of your project to get the SETTINGS_URL for your project.\n"
              + "In case you just want to test IDEasy you may simply hit return to install the default settings.\n" + "Settings URL ["
              + IdeContext.DEFAULT_SETTINGS_REPO_URL + "]:";
          repository = this.context.askForInput(message, IdeContext.DEFAULT_SETTINGS_REPO_URL);
        } else if ("-".equals(repository)) {
          repository = IdeContext.DEFAULT_SETTINGS_REPO_URL;
        }
        gitContext.pullOrClone(GitUrl.of(repository), settingsPath);
      }
      this.context.getGitContext().saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
      step.success("Successfully updated settings repository.");
    } finally {
      if (step != null) {
        step.close();
      }
    }
  }

  private void updateSoftware() {

    if (this.skipTools.isTrue()) {
      this.context.info("Skipping installation/update of tools as specified by the user.");
      return;
    }
    try (Step step = this.context.newStep("Install or update software")) {
      Set<ToolCommandlet> toolCommandlets = new HashSet<>();
      // installed tools in IDE_HOME/software
      List<Path> softwarePaths = this.context.getFileAccess().listChildren(this.context.getSoftwarePath(), Files::isDirectory);
      for (Path softwarePath : softwarePaths) {
        String toolName = softwarePath.getFileName().toString();
        ToolCommandlet toolCommandlet = this.context.getCommandletManager().getToolCommandlet(toolName);
        if (toolCommandlet != null) {
          toolCommandlets.add(toolCommandlet);
        }
      }

      // regular tools in $IDE_TOOLS
      List<String> regularTools = IdeVariables.IDE_TOOLS.get(this.context);
      if (regularTools != null) {
        for (String regularTool : regularTools) {
          toolCommandlets.add(this.context.getCommandletManager().getRequiredToolCommandlet(regularTool));
        }
      }

      // custom tools in ide-custom-tools.json
      for (CustomToolMetadata customTool : this.context.getCustomToolRepository().getTools()) {
        CustomToolCommandlet customToolCommandlet = new CustomToolCommandlet(this.context, customTool);
        toolCommandlets.add(customToolCommandlet);
      }

      // update/install the toolCommandlets
      for (ToolCommandlet toolCommandlet : toolCommandlets) {
        try {
          toolCommandlet.install(false);
        } catch (Exception e) {
          step.error(e, "Installation of {} failed!", toolCommandlet.getName());
        }
      }
      step.success();
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

}
