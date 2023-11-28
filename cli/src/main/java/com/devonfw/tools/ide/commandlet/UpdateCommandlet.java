package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.repo.CustomTool;
import com.devonfw.tools.ide.tool.CustomToolCommandlet;
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

    //updateSettings();
    //updateSoftware();
    //updateRepositories();
    testMethod();

  }

  private void testMethod() {

    for (CustomTool ct : this.context.getCustomToolRepository().getTools()) {
      this.context.info("Tool: {}, Version: {}, URL: {}", ct.getTool(), ct.getVersion(), ct.getUrl());
    }

  }

  private void updateSettings() {

    this.context.info("Updating settings repository ...");
    Path settingsPath = this.context.getSettingsPath();
    if (Files.isDirectory(settingsPath)) {
      // perform git pull on the settings repo
      this.context.gitPullOrClone(settingsPath, "https://github.com/devonfw/ide-settings");
    } else {
      throw new IllegalStateException("Cannot find settings repository.");
    }
  }

  private void updateSoftware() {

    Set<ToolCommandlet> toolCommandlets = new HashSet<>();
    Set<String> failedInstalls = new HashSet<>();

    // installed tools
    List<Path> softwares = this.context.getFileAccess().getFilesInDir(this.context.getSoftwarePath(), p -> true);
    for (Path software : softwares) {
      String toolName = software.getFileName().toString();
      toolCommandlets.add(this.context.getCommandletManager().getToolCommandlet(toolName));
    }

    // regular tools in $IDE_TOOLS
    String regularToolsString = this.context.getVariables().get("IDE_TOOLS");
    String[] regularTools = regularToolsString.split(",\\s");
    for (String regularTool : regularTools) {
      toolCommandlets.add(this.context.getCommandletManager().getToolCommandlet(regularTool));
    }

    // custom tools
    for (CustomTool customTool : this.context.getCustomToolRepository().getTools()) {
      CustomToolCommandlet customToolCommandlet = new CustomToolCommandlet(this.context, customTool);
      toolCommandlets.add(customToolCommandlet);
    }

    // update/install the toolCommandlets
    for (ToolCommandlet toolCommandlet : toolCommandlets) {
      try {
        this.context.step("Setting up {}", toolCommandlet.getName());
        toolCommandlet.install(false);
      } catch (Exception e) {
        failedInstalls.add(toolCommandlet.getName());
      }
    }

    if (failedInstalls.isEmpty()) {
      this.context.success("All tools were installed successfully.");
    } else {
      this.context.warning("Following tools could not be installed: {}", String.join(", ", failedInstalls));
    }
  }

  private void downloadAndInstall(CustomTool customTool) {

    this.context.getCustomToolRepository().download(customTool.getTool(), customTool.getEdition(), customTool.getVersion());

  }

  private void updateRepositories() {
    this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class).run();
  }
}
