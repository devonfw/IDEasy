package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.common.StepContainer;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.repo.CustomTool;
import com.devonfw.tools.ide.tool.CustomToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.variable.IdeVariables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * {@link Commandlet} to update settings, software and repositories
 */
public class UpdateCommandlet extends Commandlet {

  private static final String DEFAULT_SETTINGS_REPO_URL = "https://github.com/devonfw/ide-settings";

  private final StringProperty settingsRepo;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpdateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    settingsRepo = add(new StringProperty("", false, "settingsRepository"));
  }

  @Override
  public String getName() {

    return "update";
  }

  @Override
  public void run() {

    updateSettings();
    this.context.getFileAccess().mkdirs(this.context.getWorkspacePath());
    Path templatesFolder = this.context.getSettingsPath().resolve(IdeContext.FOLDER_TEMPLATES);
    if (!Files.exists(templatesFolder)) {
      Path legacyTemplatesFolder = this.context.getSettingsPath().resolve(IdeContext.FOLDER_LEGACY_TEMPLATES);
      if (Files.exists(legacyTemplatesFolder)) {
        templatesFolder = legacyTemplatesFolder;
      } else {
        this.context.warning("Templates folder is missing in settings folder.");
        return;
      }
    }
    setupConf(templatesFolder, this.context.getIdeHome());
    updateSoftware();
    updateRepositories();
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
            this.context.info("Copying template {} to {}.", child, confPath);
            this.context.getFileAccess().copy(child, confPath);
          }
        }
      }
    }
  }

  private void updateSettings() {

    this.context.info("Updating settings repository ...");
    Path settingsPath = this.context.getSettingsPath();
    if (Files.isDirectory(settingsPath)) {
      // perform git pull on the settings repo
      this.context.getGitContext().pull(settingsPath);
      this.context.success("Successfully updated settings repository.");
    } else {
      // check if a settings repository is given then clone, otherwise prompt user for a repository.
      String repository = settingsRepo.getValue();
      if (repository == null) {
        if (this.context.isBatchMode()) {
          repository = DEFAULT_SETTINGS_REPO_URL;
        } else {
          this.context.info("Missing your settings at {} and no SETTINGS_URL is defined.", settingsPath);
          this.context.info("Further details can be found here:");
          this.context.info("https://github.com/devonfw/IDEasy/blob/main/documentation/settings.asciidoc");
          this.context.info("Please contact the technical lead of your project to get the SETTINGS_URL for your project.");
          this.context.info("In case you just want to test IDEasy you may simply hit return to install the default settings.");
          this.context.info("");
          this.context.info("Settings URL [{}]:", DEFAULT_SETTINGS_REPO_URL);
          repository = this.context.readLine();
        }
      }
      if (repository.isBlank()) {
        repository = DEFAULT_SETTINGS_REPO_URL;
      }
      this.context.getGitContext().pullOrClone(repository, settingsPath);
      this.context.success("Successfully cloned settings repository.");
    }
  }

  private void updateSoftware() {

    Set<ToolCommandlet> toolCommandlets = new HashSet<>();

    // installed tools in IDE_HOME/software
    List<Path> softwares = this.context.getFileAccess().listChildren(this.context.getSoftwarePath(), Files::isDirectory);
    for (Path software : softwares) {
      String toolName = software.getFileName().toString();
      ToolCommandlet toolCommandlet = this.context.getCommandletManager().getToolCommandletOrNull(toolName);
      if (toolCommandlet != null) {
        toolCommandlets.add(this.context.getCommandletManager().getToolCommandletOrNull(toolName));
      }
    }

    // regular tools in $IDE_TOOLS
    List<String> regularTools =  IdeVariables.IDE_TOOLS.get(this.context);
    if (regularTools != null) {
      for (String regularTool : regularTools) {
        toolCommandlets.add(this.context.getCommandletManager().getToolCommandlet(regularTool));
      }
    }

    // custom tools
    for (CustomTool customTool : this.context.getCustomToolRepository().getTools()) {
      CustomToolCommandlet customToolCommandlet = new CustomToolCommandlet(this.context, customTool);
      toolCommandlets.add(customToolCommandlet);
    }

    // update/install the toolCommandlets
    StepContainer container = new StepContainer(this.context);
    for (ToolCommandlet toolCommandlet : toolCommandlets) {
      try {
        container.startStep(toolCommandlet.getName());
        toolCommandlet.install(false);
        container.endStep(toolCommandlet.getName(), true, null);
      } catch (Exception e) {
        container.endStep(toolCommandlet.getName(), false, e);
      }
    }
    // summary
    container.complete();
  }

  private void updateRepositories() {
    this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class).run();
  }
}
