package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesFile;
import com.devonfw.tools.ide.merge.DirectoryMerger;
import com.devonfw.tools.ide.repo.CustomToolsJson;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * {@link Commandlet} to upgrade settings after a migration from devonfw-ide to IDEasy.
 */
public class UpgradeSettingsCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpgradeSettingsCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "upgrade-settings";
  }

  @Override
  public void run() {
    updateProperties();
    replaceLegacyVariablesAndBracketsInWorkspace();
    checkIfLegacyFolderExists();
  }

  private void checkIfLegacyFolderExists() {
    this.context.info("Scanning for legacy folders...");

    Path settingsPath = context.getSettingsPath();

    Path devonFolder = settingsPath.resolve("devon");

    Path templatesFolder = settingsPath.resolve("templates");

    Path projectsFolder = settingsPath.resolve("projects");

    Path repositoriesFolder = settingsPath.resolve("repositories");

    if (Files.exists(devonFolder) && Files.isDirectory(devonFolder)) {
      try {
        if (!Files.exists(templatesFolder)) {
          Files.move(devonFolder, templatesFolder, StandardCopyOption.REPLACE_EXISTING);
          this.context.success("Successfully updated folder name from 'settings/devon' to 'settings/templates'.");
        }
      } catch (IOException e) {
        this.context.error("Error updating 'settings/devon' folder to 'settings/templates': " + e.getMessage());
      }
    } else {
      this.context.warning("The 'templates' folder already exists, skipping renaming.");
    }
    if (Files.exists(projectsFolder) && Files.isDirectory(projectsFolder)) {
      try {
        if (!Files.exists(repositoriesFolder)) {
          Files.move(projectsFolder, repositoriesFolder, StandardCopyOption.REPLACE_EXISTING);
          this.context.success("Successfully updated folder name from 'settings/projects' to 'settings/repositories'.");
        }
      } catch (IOException e) {
        this.context.error("Error updating 'settings/projects' folder to 'settings/repositories': " + e.getMessage());
      }
    } else {
      this.context.warning("The 'repositories' folder already exists, skipping renaming.");
    }
  }

  private void replaceLegacyVariablesAndBracketsInWorkspace() {
    this.context.info("Scanning for legacy variables...");

    DirectoryMerger merger = this.context.getWorkspaceMerger();
    Path settingsDir = this.context.getSettingsPath();
    Path workspaceDir = settingsDir.resolve(IdeContext.FOLDER_WORKSPACE);
    if (Files.isDirectory(workspaceDir)) {
      merger.upgrade(workspaceDir);
    }
    this.context.getFileAccess().listChildrenMapped(settingsDir, child -> {
      Path childWorkspaceDir = child.resolve(IdeContext.FOLDER_WORKSPACE);
      if (Files.isDirectory(childWorkspaceDir)) {
        merger.upgrade(childWorkspaceDir);
      }
      return null;
    });
  }

  private void updateProperties() {
    EnvironmentVariables environmentVariables = context.getVariables();
    CustomToolsJson customToolsJson = null;

    // updates DEVON_IDE_CUSTOM_TOOLS to new ide-custom-tools.json
    String devonCustomToolsName = IdeVariables.DEVON_IDE_CUSTOM_TOOLS.getName();
    String devonCustomTools = environmentVariables.getParent().get(devonCustomToolsName);
    if (devonCustomTools != null) {
      String customToolsContent = environmentVariables.getParent().get(devonCustomToolsName);
      if (!customToolsContent.isEmpty()) {
        customToolsJson = CustomToolsJson.retrieveCustomToolsFromLegacyConfig(customToolsContent, context);
      }
      if (customToolsJson != null) {
        customToolsJson.doSave(context.getSettingsPath().resolve(IdeContext.FILE_CUSTOM_TOOLS));
      }
    }

    while (environmentVariables != null) {

      if (environmentVariables instanceof EnvironmentVariablesPropertiesFile) {
        if (environmentVariables.getLegacyPropertiesFilePath() == null && environmentVariables.getPropertiesFilePath() == null) {
          continue;
        }
        Path propertiesFilePath = environmentVariables.getPropertiesFilePath();

        // adds disabled legacySupportEnabled variable if missing in ide.properties
        String legacySupportEnabledName = IdeVariables.IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED.getName();
        String legacySupportEnabled = environmentVariables.get(legacySupportEnabledName);
        if (legacySupportEnabled == null && propertiesFilePath != null && propertiesFilePath.endsWith(EnvironmentVariables.LEGACY_PROPERTIES)) {
          environmentVariables.set(legacySupportEnabledName, "false", false);
        }

        if (propertiesFilePath != null && propertiesFilePath.endsWith(EnvironmentVariables.LEGACY_PROPERTIES)) {
          environmentVariables.remove(devonCustomToolsName);
          environmentVariables.save();
        }

      }
      environmentVariables = environmentVariables.getParent();
    }
  }
}
