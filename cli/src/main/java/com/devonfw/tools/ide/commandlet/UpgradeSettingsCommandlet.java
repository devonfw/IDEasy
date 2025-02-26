package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesFile;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.merge.DirectoryMerger;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.repository.CustomToolsJson;
import com.devonfw.tools.ide.tool.repository.CustomToolsJsonMapper;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;

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

  private void updateLegacyFolders() {
    this.context.info("Updating legacy folders if present...");
    Path settingsPath = context.getSettingsPath();
    updateLegacyFolder(settingsPath, IdeContext.FOLDER_LEGACY_REPOSITORIES, IdeContext.FOLDER_REPOSITORIES);
    updateLegacyFolder(settingsPath, IdeContext.FOLDER_LEGACY_TEMPLATES, IdeContext.FOLDER_TEMPLATES);
    updateLegacyFolder(settingsPath.resolve(IdeContext.FOLDER_TEMPLATES).resolve(IdeContext.FOLDER_CONF), Mvn.MVN_CONFIG_LEGACY_FOLDER, Mvn.MVN_CONFIG_FOLDER);
  }

  private void updateLegacyFolder(Path folder, String legacyName, String newName) {
    FileAccess fileAccess = this.context.getFileAccess();

    Path legacyFolder = folder.resolve(legacyName);
    Path newFolder = folder.resolve(newName);
    if (fileAccess.isExpectedFolder(legacyFolder)) {
      try {
        if (!fileAccess.exists(newFolder)) {
          fileAccess.move(legacyFolder, newFolder);
          this.context.success("Successfully renamed folder '{}' to '{}' in {}.", legacyName, newName, folder);
        }
      } catch (IllegalStateException e) {
        this.context.error(e, "Error renaming folder {} to {} in {}", legacyName, newName, folder);
      }
    }
  }

  private void updateWorkspaceTemplates() {
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

  private void updateProperties() {
    // updates DEVON_IDE_CUSTOM_TOOLS to new ide-custom-tools.json
    String devonCustomTools = IdeVariables.DEVON_IDE_CUSTOM_TOOLS.get(this.context);
    if (devonCustomTools != null) {
      CustomToolsJson customToolsJson = CustomToolsJsonMapper.parseCustomToolsFromLegacyConfig(devonCustomTools, context);
      if (customToolsJson != null) {
        CustomToolsJsonMapper.saveJson(customToolsJson, this.context.getSettingsPath().resolve(IdeContext.FILE_CUSTOM_TOOLS));
      }
    }

    // update properties (devon.properties -> ide.properties, convert legacy properties)
    EnvironmentVariables environmentVariables = context.getVariables();
    while (environmentVariables != null) {
      if (environmentVariables instanceof EnvironmentVariablesPropertiesFile environmentVariablesProperties) {
        updateProperties(environmentVariablesProperties);
      }
      environmentVariables = environmentVariables.getParent();
    }
    Path templatePropertiesDir = this.context.getSettingsTemplatePath().resolve(IdeContext.FOLDER_CONF);
    if (Files.exists(templatePropertiesDir)) {
      EnvironmentVariablesPropertiesFile environmentVariablesProperties = new EnvironmentVariablesPropertiesFile(null, EnvironmentVariablesType.CONF,
          templatePropertiesDir, null, this.context);
      updateProperties(environmentVariablesProperties);
    }
  }

  private void updateProperties(EnvironmentVariablesPropertiesFile environmentVariables) {
    Path propertiesFilePath = environmentVariables.getPropertiesFilePath();
    if (environmentVariables.getLegacyConfiguration() != null) {
      if (environmentVariables.getType() == EnvironmentVariablesType.SETTINGS) {
        // adds disabled legacySupportEnabled variable if missing in ide.properties
        environmentVariables.set(IdeVariables.IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED.getName(), "false", false);
      }
      environmentVariables.remove(IdeVariables.DEVON_IDE_CUSTOM_TOOLS.getName());
      for (VariableDefinition<?> var : IdeVariables.VARIABLES) {
        String legacyName = var.getLegacyName();
        if (legacyName != null) {
          String value = environmentVariables.get(legacyName);
          if (value != null) {
            String name = var.getName();
            String newValue = environmentVariables.get(name);
            if (newValue == null) {
              environmentVariables.set(name, value, environmentVariables.isExported(name));
            }
          }
          environmentVariables.remove(legacyName);
        }
      }
      updatePropertiesLegacyEdition(environmentVariables, "INTELLIJ_EDITION_TYPE", "INTELLIJ_EDITION", this::mapLegacyIntellijEdition);
      updatePropertiesLegacyEdition(environmentVariables, "ECLIPSE_EDITION_TYPE", "ECLIPSE_EDITION", this::mapLegacyEclipseEdition);
      environmentVariables.save();
      this.context.getFileAccess().backup(environmentVariables.getLegacyPropertiesFilePath());
    }
  }

  private String mapLegacyIntellijEdition(String legacyEdition) {

    return switch (legacyEdition) {
      case "U" -> "ultimate";
      case "C" -> "intellij";
      default -> {
        this.context.warning("Undefined legacy edition {}", legacyEdition);
        yield "intellij";
      }
    };
  }

  private String mapLegacyEclipseEdition(String legacyEdition) {

    return switch (legacyEdition) {
      case "java" -> "eclipse";
      case "jee" -> "jee";
      case "cpp" -> "cpp";
      default -> {
        this.context.warning("Undefined legacy edition {}", legacyEdition);
        yield "eclipse";
      }
    };
  }

  private static void updatePropertiesLegacyEdition(EnvironmentVariablesPropertiesFile environmentVariables, String legacyEditionVariable,
      String newEditionVariable, Function<String, String> editionMapper) {

    String legacyEdition = environmentVariables.get(legacyEditionVariable);
    if (legacyEdition != null) {
      String newEdition = environmentVariables.get(newEditionVariable);
      if (newEdition == null) {
        environmentVariables.set(newEditionVariable, editionMapper.apply(legacyEdition), false);
      }
      environmentVariables.remove(legacyEditionVariable);
    }
  }
}
