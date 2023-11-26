package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateCommandlet extends Commandlet {

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

   // updateSettings();
    updateSoftware();
    updateRepositories();

  }

  private void updateRepositories() {
    this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class).run();

  }

  private void updateSoftware() {

    // Retrieve all installed software
    List<Path> softwares = this.context.getFileAccess().getFilesInDir(this.context.getSoftwarePath(), p -> true);
    Set<ToolCommandlet> toolCommandlets = new HashSet<>();
    for (Path software : softwares) {
      toolCommandlets.add(this.context.getCommandletManager().getToolCommandlet(software.getFileName().toString()));
    }

    String regularToolsV = this.context.getVariables().get("IDE_TOOLS");
    // Split the string based on comma delimiter
    String[] regularTools = regularToolsV.split(",\\s");
    for (String regularTool : regularTools) {
      toolCommandlets.add(this.context.getCommandletManager().getToolCommandlet(regularTool));
    }

    // update/install the toolCommandlets
    for (ToolCommandlet toolCommandlet : toolCommandlets) {
      toolCommandlet.install();
    }


  }

  private void updateSettings() {

    Path settingsPath = this.context.getSettingsPath();
    if (Files.isDirectory(settingsPath)) {
      // perform git pull on the settings repo
      this.context.gitPullOrClone(settingsPath, "http");
    } else {
      throw new IllegalStateException("Cannot find settings repository.");
    }
  }
}
