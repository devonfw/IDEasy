package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.repo.CustomTool;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.CustomToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Abstract {@link Commandlet} base-class for both {@link UpdateCommandlet} and {@link CreateCommandlet}.
 */
public abstract class AbstractUpdateCommandlet extends Commandlet {

  /** {@link StringProperty} for the settings repository URL. */
  protected final StringProperty settingsRepo;

  /** {@link FlagProperty} for skipping installation/updating of tools */
  protected final FlagProperty skipTools;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public AbstractUpdateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.skipTools = add(new FlagProperty("--skip-tools", false, null));
    this.settingsRepo = new StringProperty("", false, "settingsRepository");
  }

  @Override
  public void run() {

    updateSettings();
    updateConf();
    reloadContext();

    if (this.skipTools.isTrue()) {
      this.context.info("Skipping installation/update of tools as specified by the user.");
    } else {
      updateSoftware();
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
   * Updates the settings repository in IDE_HOME/settings by either cloning if no such repository exists or pulling
   * if the repository exists then saves the latest current commit ID in the file ".commit.id".
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
      for (CustomTool customTool : this.context.getCustomToolRepository().getTools()) {
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

}
