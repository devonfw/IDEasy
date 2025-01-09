package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesFile;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.merge.DirectoryMerger;
import com.devonfw.tools.ide.repo.CustomToolsJson;
import com.devonfw.tools.ide.repo.CustomToolsJsonMapper;
import com.devonfw.tools.ide.tool.mvn.Mvn;
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
    updateLegacyFolders();
    updateProperties();
    updateWorkspaceTemplates();
  }

  void updateLegacyFolders() {
    this.context.info("Updating legacy folders if present...");
    Path settingsPath = context.getSettingsPath();
    updateLegacyFolder(settingsPath, IdeContext.FOLDER_LEGACY_REPOSITORIES, IdeContext.FOLDER_REPOSITORIES);
    updateLegacyFolder(settingsPath, IdeContext.FOLDER_LEGACY_TEMPLATES, IdeContext.FOLDER_TEMPLATES);
    updateLegacyFolder(settingsPath.resolve(IdeContext.FOLDER_TEMPLATES).resolve(IdeContext.FOLDER_CONF), Mvn.MVN_CONFIG_LEGACY_FOLDER, Mvn.MVN_CONFIG_FOLDER);
  }

  private void updateLegacyFolder(Path folder, String legacyName, String newName) {

    Path legacyFolder = folder.resolve(legacyName);
    Path newFolder = folder.resolve(newName);
    if (Files.isDirectory(legacyFolder)) {
      try {
        if (!Files.exists(newFolder)) {
          Files.move(legacyFolder, newFolder, StandardCopyOption.REPLACE_EXISTING);
          this.context.success("Successfully renamed folder '{}' to '{}' in {}.", legacyName, newName, folder);
        }
      } catch (IOException e) {
        this.context.error("Error renaming folder " + legacyName + " to " + newName + " in " + folder, e);
      }
    }
  }

  void updateWorkspaceTemplates() {
    this.context.info("Updating workspace templates (replace legacy variables and change variable syntax)...");

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

  void updateProperties() {
    // updates DEVON_IDE_CUSTOM_TOOLS to new ide-custom-tools.json
    String devonCustomTools = IdeVariables.DEVON_IDE_CUSTOM_TOOLS.get(this.context);
    if (devonCustomTools != null) {
      CustomToolsJson customToolsJson = CustomToolsJsonMapper.parseCustomToolsFromLegacyConfig(devonCustomTools, context);
      if (customToolsJson != null) {
        CustomToolsJsonMapper.saveJson(customToolsJson, this.context.getSettingsPath().resolve(IdeContext.FILE_CUSTOM_TOOLS));
      }
    }

    EnvironmentVariables environmentVariables = context.getVariables();
    while (environmentVariables != null) {
      if (environmentVariables instanceof EnvironmentVariablesPropertiesFile environmentVariablesProperties) {
        updateProperties(environmentVariablesProperties);
      }
      environmentVariables = environmentVariables.getParent();
    }
    Path templateProperties = this.context.getSettingsTemplatePath().resolve(IdeContext.FOLDER_CONF).resolve(EnvironmentVariables.LEGACY_PROPERTIES);
    if (Files.exists(templateProperties)) {
      EnvironmentVariablesPropertiesFile environmentVariablesProperties = new EnvironmentVariablesPropertiesFile(null, EnvironmentVariablesType.USER,
          templateProperties, this.context);
      updateProperties(environmentVariablesProperties);
    }

  }

  private static void updateProperties(EnvironmentVariablesPropertiesFile environmentVariables) {
    Path propertiesFilePath = environmentVariables.getPropertiesFilePath();
    if (propertiesFilePath != null || environmentVariables.getLegacyPropertiesFilePath() != null) {
      if (environmentVariables.getType() == EnvironmentVariablesType.SETTINGS) {
        // adds disabled legacySupportEnabled variable if missing in ide.properties
        String legacySupportEnabledName = IdeVariables.IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED.getName();
        String legacySupportEnabledValue = environmentVariables.get(legacySupportEnabledName);
        if (!"false".equals(legacySupportEnabledValue)) {
          environmentVariables.set(legacySupportEnabledName, "false", false);
        }
      }
      if ((propertiesFilePath != null) && propertiesFilePath.endsWith(EnvironmentVariables.LEGACY_PROPERTIES)) {
        environmentVariables.remove(IdeVariables.DEVON_IDE_CUSTOM_TOOLS.getName());
      }
      environmentVariables.save();
    }
  }
}
