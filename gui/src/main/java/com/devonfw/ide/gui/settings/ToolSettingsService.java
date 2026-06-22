package com.devonfw.ide.gui.settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.MainController;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesFile;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Service that exposes tool configuration (read/write) based on the existing environment variables machinery.
 * <p>
 * - Reads IDE_TOOLS to detect enabled tools - Reads per-tool _VERSION and _EDITION variables - Writes changes into the settings variables and saves them
 */
public final class ToolSettingsService {

  private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

  /**
   * Retrieve a list of ToolConfiguration objects for all locally supported tools discovered via the {@link CommandletManager} of the provided
   * {@link IdeGuiContext}.
   */
  public List<ToolConfiguration> listToolConfigurations(IdeGuiContext guiContext) {
    List<String> enabledTools = IdeVariables.IDE_TOOLS.get(guiContext);

    List<ToolConfiguration> result = new ArrayList<>();
    for (Commandlet commandlet : guiContext.getCommandletManager().getCommandlets()) {
      if (commandlet instanceof LocalToolCommandlet localToolCommandlet) {
        ToolConfiguration toolConfiguration = createFromCommandlet(localToolCommandlet, enabledTools);
        result.add(toolConfiguration);
      }
    }
    result.sort(Comparator.comparing(ToolConfiguration::getToolName, String.CASE_INSENSITIVE_ORDER));
    return result;
  }

  private ToolConfiguration createFromCommandlet(ToolCommandlet commandlet, List<String> enabledTools) {
    String toolName = commandlet.getName();
    ToolConfiguration toolConfiguration = new ToolConfiguration(toolName);
    // configured version/edition as strings (null when not set)
    VersionIdentifier version = commandlet.getConfiguredVersion();
    toolConfiguration.setConfiguredVersion(version == null ? null : version.toString());
    toolConfiguration.setConfiguredEdition(commandlet.getConfiguredEdition());
    // A tool is enabled only if listed in IDE_TOOLS
    boolean enabled = (enabledTools != null && enabledTools.stream().anyMatch(s -> s.equalsIgnoreCase(toolName)));
    toolConfiguration.setEnabled(enabled);
    try {
      List<String> editions = commandlet.getToolRepository().getSortedEditions(toolName);
      toolConfiguration.setAvailableEditions(editions);
      toolConfiguration.setSupportsEdition(editions != null && editions.size() > 1);
    } catch (Exception e) {
      toolConfiguration.setAvailableEditions(List.of());
      toolConfiguration.setSupportsEdition(false);
    }
    // versions support: ask repository for available versions
    // If tool has editions, versions are loaded dynamically when edition is selected
    // If tool has no editions, load versions now for the configured edition
    try {
      String editionForVersions = null;
      if (!toolConfiguration.isSupportsEdition()) {
        // No edition support: use configured edition (may be null)
        editionForVersions = toolConfiguration.getConfiguredEdition();
      } else {
        // Has edition support: if an edition is already configured, load versions for it
        editionForVersions = toolConfiguration.getConfiguredEdition();
        if (editionForVersions == null || editionForVersions.isBlank()) {
          // No edition selected yet: versions will be loaded dynamically when user selects one
          toolConfiguration.setAvailableVersions(List.of());
          return toolConfiguration;
        }
      }
      // Load versions for the determined edition
      List<VersionIdentifier> versionIds = commandlet.getToolRepository().getSortedVersions(toolName, editionForVersions, commandlet);
      List<String> versionStrings = versionIds.stream().map(VersionIdentifier::toString).collect(Collectors.toList());
      toolConfiguration.setAvailableVersions(versionStrings);
    } catch (Exception e) {
      toolConfiguration.setAvailableVersions(List.of());
    }
    return toolConfiguration;
  }

  /**
   * Build the ide.properties content that would result from applying the given configurations. This is a plain textual preview and does not attempt to preserve
   * comments or formatting.
   *
   * @param configs the tool configurations
   * @return the generated properties content
   */
  public String buildSettingsContent(List<ToolConfiguration> configs) {
    StringBuilder sb = new StringBuilder();
    // IDE_TOOLS
    List<String> enabledTools = configs.stream().filter(ToolConfiguration::isEnabled)
        .map(tc -> tc.getToolName().toLowerCase(Locale.ROOT)).collect(Collectors.toList());
    sb.append(IdeVariables.IDE_TOOLS.getName()).append("=").append(String.join(", ", enabledTools)).append(System.lineSeparator());
    sb.append(System.lineSeparator());
    // per-tool variables
    for (ToolConfiguration toolConfiguration : configs) {
      if (!toolConfiguration.isEnabled()) {
        continue;
      }
      String tool = toolConfiguration.getToolName();
      String versionVar = EnvironmentVariables.getToolVersionVariable(tool);
      String editionVar = EnvironmentVariables.getToolEditionVariable(tool);
      String v = toolConfiguration.getConfiguredVersion();
      String e = toolConfiguration.getConfiguredEdition();
      if (v != null && !v.isBlank()) {
        sb.append(versionVar).append("=").append(v).append(System.lineSeparator());
      }
      if (toolConfiguration.isSupportsEdition()) {
        if (e != null && !e.isBlank()) {
          sb.append(editionVar).append("=").append(e).append(System.lineSeparator());
        }
      }
    }
    return sb.toString();
  }

  /**
   * Reload available versions for a tool when the edition changes. This is called from the UI when the user selects/changes an edition.
   *
   * @param tool the tool name
   * @param selectedEdition the newly selected edition
   * @param guiContext the current GUI context
   * @return the list of available versions for that edition, or empty list on error
   */
  public List<String> reloadVersionsForEdition(String tool, String selectedEdition, IdeGuiContext guiContext) {
    try {
      CommandletManager cm = guiContext.getCommandletManager();
      // Find the commandlet for this tool
      ToolCommandlet toolCmd = cm.getToolCommandlet(tool);

      if (toolCmd == null) {
        return List.of();
      }
      // Fetch versions for this specific edition
      if (selectedEdition == null || selectedEdition.isBlank()) {
        return List.of();
      }
      List<VersionIdentifier> versionIds = toolCmd.getToolRepository().getSortedVersions(tool, selectedEdition, toolCmd);
      return versionIds.stream().map(VersionIdentifier::toString).collect(Collectors.toList());
    } catch (Exception e) {
      return List.of();
    }
  }

  /**
   * Apply and persist the provided tool configurations into the settings (ide.properties) file.
   * <p>
   * This method performs a batch update: - updates IDE_TOOLS list - updates each tool's _VERSION and _EDITION variables (in the settings variables layer)
   */
  public void applyAndSave(List<ToolConfiguration> toolConfigurations, IdeGuiContext guiContext) {
    // write to settings layer explicitly
    EnvironmentVariables settingsVars = guiContext.getVariables().getByType(EnvironmentVariablesType.SETTINGS);

    // Create a backup of the existing properties file before applying changes, if possible
    //TODO: Needs to be discussed if this is necessary
    try {
      if (settingsVars instanceof EnvironmentVariablesPropertiesFile evpf) {
        Path path = evpf.getPropertiesFilePath();
        if (path != null && Files.exists(path)) {
          Path backup = path.resolveSibling(path.getFileName().toString() + ".bak");
          Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to create backup of the existing properties file: {}", e.getMessage());
    }

    // update IDE_TOOLS
    List<String> enabledTools = toolConfigurations.stream().filter(ToolConfiguration::isEnabled)
        .map(tc -> tc.getToolName().toLowerCase(Locale.ROOT)).collect(Collectors.toList());

    String toolsValue = String.join(", ", enabledTools);
    settingsVars.set(IdeVariables.IDE_TOOLS.getName(), toolsValue, false);

    // update versions and editions for each tool (*_VERSION & *_EDITION)
    for (ToolConfiguration toolConfiguration : toolConfigurations) {
      String tool = toolConfiguration.getToolName();
      String versionVar = EnvironmentVariables.getToolVersionVariable(tool);
      String editionVar = EnvironmentVariables.getToolEditionVariable(tool);
      if (toolConfiguration.isEnabled()) {
        // write version (null/unset -> remove by writing null)
        settingsVars.set(versionVar, emptyToNull(toolConfiguration.getConfiguredVersion()), false);
        // write edition only if the tool supports editions
        if (toolConfiguration.isSupportsEdition()) {
          settingsVars.set(editionVar, emptyToNull(toolConfiguration.getConfiguredEdition()), false);
        }
      } else {
        // tool not enabled -> remove any configured version/edition
        settingsVars.set(versionVar, null, false);
        if (toolConfiguration.isSupportsEdition()) {
          settingsVars.set(editionVar, null, false);
        }
      }
    }

    // persist once
    settingsVars.save();
  }

  private static String emptyToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

}

