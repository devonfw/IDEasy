package com.devonfw.ide.gui.core.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.context.IdeGuiContext;
import com.devonfw.ide.gui.core.model.ToolConfiguration;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Service that exposes tool configuration (read/write) based on the existing environment variables machinery.
 * <p>
 * - Reads IDE_TOOLS to detect enabled tools - Reads per-tool _VERSION and _EDITION variables - Writes changes into the settings variables and saves them
 */
public final class ToolConfigurationService {

  private static final Logger LOG = LoggerFactory.getLogger(ToolConfigurationService.class);

  /**
   * Retrieve a list of ToolConfiguration objects for all locally supported tools discovered via the {@link CommandletManager} of the provided
   * {@link IdeGuiContext}.
   */
  public List<ToolConfiguration> listToolConfigurations(IdeGuiContext guiContext) {
    List<String> enabledTools = IdeVariables.IDE_TOOLS.get(guiContext);

    List<ToolConfiguration> toolConfigurations = new ArrayList<>();
    for (Commandlet commandlet : guiContext.getCommandletManager().getCommandlets()) {
      if (commandlet instanceof AbstractToolCommandlet toolCmd) {
        toolConfigurations.add(toToolConfiguration(toolCmd, enabledTools));
      }
    }

    toolConfigurations.sort(Comparator.comparing(ToolConfiguration::getToolName, String.CASE_INSENSITIVE_ORDER));
    return toolConfigurations;
  }

  ToolConfiguration toToolConfiguration(AbstractToolCommandlet commandlet, List<String> enabledTools) {
    String toolName = commandlet.getName();
    ToolConfiguration toolConfiguration = new ToolConfiguration(toolName);
    toolConfiguration.setConfiguredVersion(toConfiguredVersionString(commandlet.getConfiguredVersion()));
    toolConfiguration.setConfiguredEdition(commandlet.getConfiguredEdition());
    toolConfiguration.setEnabled(isEnabledTool(toolName, enabledTools));

    return toolConfiguration;
  }

  private String toConfiguredVersionString(VersionIdentifier version) {
    return version == null ? null : version.toString();
  }

  private boolean isEnabledTool(String toolName, List<String> enabledTools) {
    return enabledTools != null && enabledTools.stream().anyMatch(name -> name.equalsIgnoreCase(toolName));
  }

  private List<String> getEnabledToolNames(List<ToolConfiguration> configs) {
    return configs.stream().filter(ToolConfiguration::isEnabled).map(tc -> tc.getToolName().toLowerCase(Locale.ROOT)).collect(Collectors.toList());
  }

  /**
   * Reload available versions for a tool when the edition changes. This is called from the UI when the user selects/changes an edition.
   *
   * @param tool the tool name
   * @param context the current IDE context
   * @return the list of available edition for that tool
   */
  public List<String> loadEditionsForTool(String tool, IdeContext context) {
    try {
      AbstractToolCommandlet cmd = context.getCommandletManager().getToolCommandlet(tool);
      if (cmd == null) {
        return List.of();
      }
      return cmd.getToolRepository().getSortedEditions(tool);
    } catch (Exception e) {
      LOG.debug("Failed to load editions for tool {}: {}", tool, e.getMessage(), e);
      return List.of();
    }
  }

  /**
   * Reload available versions for a tool when the edition changes. This is called from the UI when the user selects/changes an edition.
   *
   * @param tool the tool name
   * @param selectedEdition the newly selected edition
   * @param context the current IDE context
   * @return the list of available versions for that edition, or empty list on error
   */
  public List<String> loadVersionsForSelectedEdition(String tool, String selectedEdition, IdeContext context) {
    try {
      AbstractToolCommandlet toolCmd = context.getCommandletManager().getToolCommandlet(tool);

      if (toolCmd == null) {
        return List.of();
      }
      if (selectedEdition == null || selectedEdition.isBlank()) {
        return List.of();
      }
      List<VersionIdentifier> versionIds = toolCmd.getToolRepository().getSortedVersions(tool, selectedEdition, toolCmd);
      return versionIds.stream().map(VersionIdentifier::toString).collect(Collectors.toList());
    } catch (Exception e) {
      LOG.debug("Failed to load versions for tool {}: {}", tool, e.getMessage(), e);
      return List.of();
    }
  }

  /**
   * Validate a user-entered version against a tool's loaded available versions.
   * <p>
   * Exact versions are checked for literal presence in {@code availableVersions}. Version patterns (e.g. {@code 2026.1*} or {@code 2026*!}, see
   * {@link VersionIdentifier#isPattern()}) are considered valid if they {@link VersionIdentifier#matches(VersionIdentifier) match} at least one available
   * version. Blank input and {@code "*"} are always valid, and validation is skipped (treated as valid) when {@code availableVersions} has not been loaded
   * yet.
   *
   * @param enteredVersion the version (or version pattern) typed by the user.
   * @param availableVersions the available versions for the tool/edition, or {@code null} if not loaded yet.
   * @return {@code true} if the entered version is valid, {@code false} otherwise.
   */
  public boolean isValidVersion(String enteredVersion, List<String> availableVersions) {
    if (enteredVersion == null || enteredVersion.isBlank() || enteredVersion.equals("*")) {
      return true;
    }
    if (availableVersions == null) {
      return true;
    }
    VersionIdentifier entered = VersionIdentifier.of(enteredVersion);
    if (entered == null) {
      return false;
    }
    if (entered.isPattern()) {
      return availableVersions.stream().map(VersionIdentifier::of).anyMatch(entered::matches);
    }
    return availableVersions.contains(enteredVersion);
  }

  /**
   * Apply and persist the provided tool configurations into the settings (ide.properties) file.
   * <p>
   * This method performs a batch update: - updates IDE_TOOLS list - updates each tool's _VERSION and _EDITION variables (in the settings variables layer)
   */
  public void applyAndSave(List<ToolConfiguration> toolConfigurations, IdeGuiContext guiContext) {
    EnvironmentVariables settingsVars = guiContext.getVariables().getByType(EnvironmentVariablesType.SETTINGS);

    settingsVars.set(IdeVariables.IDE_TOOLS.getName(), String.join(", ", getEnabledToolNames(toolConfigurations)), false);

    for (ToolConfiguration toolConfiguration : toolConfigurations) {
      String versionVar = EnvironmentVariables.getToolVersionVariable(toolConfiguration.getToolName());
      String editionVar = EnvironmentVariables.getToolEditionVariable(toolConfiguration.getToolName());
      if (toolConfiguration.isEnabled()) {
        settingsVars.set(versionVar, emptyToNull(toolConfiguration.getConfiguredVersion()), false);
        if (toolConfiguration.isSupportsEdition()) {
          settingsVars.set(editionVar, emptyToNull(toolConfiguration.getConfiguredEdition()), false);
        }
      } else {
        settingsVars.set(versionVar, null, false);
        if (toolConfiguration.isSupportsEdition()) {
          settingsVars.set(editionVar, null, false);
        }
      }
    }

    settingsVars.save();
  }

  private static String emptyToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

}
