package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.common.StepContainer;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.repo.CustomTool;
import com.devonfw.tools.ide.tool.CustomToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.variable.IdeVariables;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
    //updateSoftware();
    //updateRepositories();
  }


  private void updateSettings() {

    this.context.info("Updating settings repository ...");
    Path settingsPath = this.context.getSettingsPath();
    if (Files.isDirectory(settingsPath)) {
      // perform git pull on the settings repo
      this.context.gitPullOrClone(settingsPath, DEFAULT_SETTINGS_REPO_URL);
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
          this.context.info();
          repository = this.context.read("Settings URL [" + DEFAULT_SETTINGS_REPO_URL +"]: ");
        }
      }
      if (repository.isBlank()) {
        repository = DEFAULT_SETTINGS_REPO_URL;
      }
      this.context.gitPullOrClone(settingsPath, repository);
    }
  }

  private void updateSoftware() {

    Set<ToolCommandlet> toolCommandlets = new HashSet<>();

    // installed tools in IDE_HOME/software
    List<Path> softwares = this.context.getFileAccess().getChildrenInDir(this.context.getSoftwarePath(), Files::isDirectory);
    for (Path software : softwares) {
      String toolName = software.getFileName().toString();
      ToolCommandlet toolCommandlet = this.context.getCommandletManager().getToolCommandletOrNull(toolName);
      if (toolCommandlet != null) {
        toolCommandlets.add(this.context.getCommandletManager().getToolCommandletOrNull(toolName));
      }
    }

    // regular tools in $IDE_TOOLS
    List<String> regularTools =  IdeVariables.IDE_TOOLS.get(this.context);
    for (String regularTool : regularTools) {
      toolCommandlets.add(this.context.getCommandletManager().getToolCommandlet(regularTool));
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
