package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
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

  private static final String SETTINGS_REPO_URL = "https://github.com/devonfw/ide-settings";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpdateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "update";
  }

  @Override
  public void run() {

    updateSettings();
    updateSoftware();
    updateRepositories();
  }


  private void updateSettings() {

    this.context.info("Updating settings repository ...");
    Path settingsPath = this.context.getSettingsPath();
    if (Files.isDirectory(settingsPath)) {
      // perform git pull on the settings repo
      this.context.gitPullOrClone(settingsPath, SETTINGS_REPO_URL);
      this.context.success("Successfully updated settings repository.");
    } else {
      throw new IllegalStateException("Cannot find settings repository.");
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
