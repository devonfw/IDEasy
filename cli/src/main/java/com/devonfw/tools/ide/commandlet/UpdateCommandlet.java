package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.repo.CustomTool;
import com.devonfw.tools.ide.tool.CustomToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

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
    List<Path> softwares = this.context.getFileAccess().getFilesInDir(this.context.getSoftwarePath(), path -> true);
    for (Path software : softwares) {
      String toolName = software.getFileName().toString();
      try {
        toolCommandlets.add(this.context.getCommandletManager().getToolCommandlet(toolName));
      } catch (IllegalArgumentException e) {
        //tool is a custom tool, ignore ...
      }
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
    Map<String, String> tool2exception = new HashMap<>();
    for (ToolCommandlet toolCommandlet : toolCommandlets) {
      try {
        this.context.step("Setting up {}", toolCommandlet.getName());
        toolCommandlet.install(false);
      } catch (Exception e) {
        tool2exception.put(toolCommandlet.getName(), e.getMessage());
      }
    }

    if (tool2exception.isEmpty()) {
      this.context.success("All tools were successfully installed.");
    } else {
      this.context.warning("Following tools could not be installed:");
      for (String toolName : tool2exception.keySet())
      this.context.warning("{}: {}", toolName, tool2exception.get(toolName));
    }
  }

  private void updateRepositories() {
    this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class).run();
  }
}
