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

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesFile;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Service that exposes tool configuration (read/write) based on the existing environment variables machinery.
 * <p>
 * - Reads IDE_TOOLS to detect enabled tools - Reads per-tool _VERSION and _EDITION variables - Writes changes into the settings variables and saves them
 */
public final class ToolSettingsService {

  private static final Logger LOG = LoggerFactory.getLogger(ToolSettingsService.class);

  /**
   * Retrieve a list of ToolConfiguration objects for all locally supported tools discovered via the {@link CommandletManager} of the provided
   * {@link IdeGuiContext}.
   */
  public List<ToolConfiguration> listToolConfigurations(IdeGuiContext guiContext) {
    List<String> enabledTools = IdeVariables.IDE_TOOLS.get(guiContext);

    List<ToolConfiguration> toolConfigurations = new ArrayList<>();
    for (Commandlet commandlet : guiContext.getCommandletManager().getCommandlets()) {
      if (commandlet instanceof ToolCommandlet toolCmd) {
        ToolConfiguration toolConfiguration = toToolConfiguration(toolCmd, enabledTools);
        toolConfiguration.setGroup(determineGroup(toolCmd));
        toolConfigurations.add(toolConfiguration);
      }
    }

    toolConfigurations.sort(Comparator.comparing((ToolConfiguration tc) -> tc.getGroup() == null ? ToolConfiguration.ToolGroup.OTHER : tc.getGroup())
        .thenComparing(ToolConfiguration::getToolName, String.CASE_INSENSITIVE_ORDER));
    return toolConfigurations;
  }

  private ToolConfiguration.ToolGroup determineGroup(ToolCommandlet toolCmd) {
    if (toolCmd instanceof IdeToolCommandlet) {
      return ToolConfiguration.ToolGroup.IDE;
    } else if (toolCmd instanceof PipBasedCommandlet) {
      return ToolConfiguration.ToolGroup.PIP;
    } else if (toolCmd instanceof NpmBasedCommandlet) {
      return ToolConfiguration.ToolGroup.NPM;
    } else if (toolCmd instanceof GlobalToolCommandlet) {
      return ToolConfiguration.ToolGroup.GLOBAL;
    } else if (toolCmd instanceof LocalToolCommandlet) {
      return ToolConfiguration.ToolGroup.LOCAL;
    }
    return ToolConfiguration.ToolGroup.OTHER;
  }

  ToolConfiguration toToolConfiguration(ToolCommandlet commandlet, List<String> enabledTools) {
    String toolName = commandlet.getName();
    ToolConfiguration toolConfiguration = new ToolConfiguration(toolName);
    toolConfiguration.setConfiguredVersion(toConfiguredVersion(commandlet.getConfiguredVersion()));
    toolConfiguration.setConfiguredEdition(commandlet.getConfiguredEdition());
    toolConfiguration.setEnabled(isEnabledTool(toolName, enabledTools));

    return toolConfiguration;
  }

  private String toConfiguredVersion(VersionIdentifier version) {
    return version == null ? null : version.toString();
  }

  private boolean isEnabledTool(String toolName, List<String> enabledTools) {
    return enabledTools != null && enabledTools.stream().anyMatch(name -> name.equalsIgnoreCase(toolName));
  }


  /**
   * Build the ide.properties content that would result from applying the given configurations. This is a plain textual preview and does not attempt to preserve
   * comments or formatting.
   *
   * @param configs the tool configurations
   * @return the generated properties content
   */
  public String buildPreviewSettingsContent(List<ToolConfiguration> configs) {
    StringBuilder sb = new StringBuilder();
    List<String> enabledToolNames = getEnabledToolNames(configs);
    sb.append(IdeVariables.IDE_TOOLS.getName()).append("=").append(String.join(", ", enabledToolNames)).append(System.lineSeparator());
    sb.append(System.lineSeparator());
    for (ToolConfiguration toolConfiguration : configs) {
      if (!toolConfiguration.isEnabled()) {
        continue;
      }
      appendToolVariables(sb, toolConfiguration);
    }
    return sb.toString();
  }

  private List<String> getEnabledToolNames(List<ToolConfiguration> configs) {
    return configs.stream().filter(ToolConfiguration::isEnabled).map(tc -> tc.getToolName().toLowerCase(Locale.ROOT)).collect(Collectors.toList());
  }

  private void appendToolVariables(StringBuilder sb, ToolConfiguration toolConfiguration) {
    String tool = toolConfiguration.getToolName();
    String versionVar = EnvironmentVariables.getToolVersionVariable(tool);
    String editionVar = EnvironmentVariables.getToolEditionVariable(tool);
    String version = toolConfiguration.getConfiguredVersion();
    String edition = toolConfiguration.getConfiguredEdition();
    if (version != null && !version.isBlank()) {
      sb.append(versionVar).append("=").append(version).append(System.lineSeparator());
    }
    if (toolConfiguration.isSupportsEdition()) {
      if (edition != null && !edition.isBlank()) {
        sb.append(editionVar).append("=").append(edition).append(System.lineSeparator());
      }
    }
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
      ToolCommandlet cmd = context.getCommandletManager().getToolCommandlet(tool);
      if (cmd == null) {
        return List.of();
      }
      return cmd.getToolRepository().getSortedEditions(tool);
    } catch (Exception e) {
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
      ToolCommandlet toolCmd = context.getCommandletManager().getToolCommandlet(tool);

      if (toolCmd == null) {
        return List.of();
      }
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
    EnvironmentVariables settingsVars = guiContext.getVariables().getByType(EnvironmentVariablesType.SETTINGS);

    createBackupIfPossible(settingsVars);

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

  //TODO: Align with Team if this is needed
  private void createBackupIfPossible(EnvironmentVariables settingsVars) {
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
  }

  private static String emptyToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

}

