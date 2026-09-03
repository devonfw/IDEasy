package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Path;
import java.util.Collection;

import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * Interface for tools that support plugin management.
 * <p>
 * This follows the same pattern as {@link com.devonfw.tools.ide.tool.ide.IdeFeatures}, decoupling plugin capabilities from the installation mechanism. Both
 * binary-installed IDEs (VS Code, IntelliJ) and package-manager-installed tools (Spyder via pip) can support plugins by composing a {@link PluginManager}.
 * </p>
 */
public interface PluginFeatures {

  /**
   * @return the {@link ToolCommandlet#getName() name} of the tool owning the plugins.
   */
  String getName();

  /**
   * @return the {@link ToolCommandlet#getConfiguredEdition() configured edition} of the tool owning the plugins.
   */
  String getConfiguredEdition();

  /**
   * @return the {@link ToolCommandlet#getInstalledEdition() installed edition} of the tool owning the plugins or {@code null} if not installed.
   */
  String getInstalledEdition();

  /**
   * @return the {@link Path} to the folder with the plugin configuration files inside the settings.
   */
  Path getPluginsConfigPath();

  /**
   * @return the {@link Path} where the plugins of this tool shall be installed.
   */
  Path getPluginsInstallationPath();

  /**
   * @return {@code true} if the {@link ToolPluginDescriptor#url() plugin url} is needed, {@code false} otherwise.
   */
  boolean isPluginUrlNeeded();

  /**
   * @return the {@link ToolPlugins} configured for this tool.
   */
  ToolPlugins getPlugins();

  /**
   * @param key the filename of the properties file configuring the requested plugin (typically excluding the ".properties" extension).
   * @return the {@link ToolPluginDescriptor} for the given {@code key}.
   */
  ToolPluginDescriptor getPlugin(String key);

  /**
   * Installs the given active plugins and handles the inactive ones.
   *
   * @param plugins the {@link Collection} of {@link ToolPluginDescriptor plugins} to install.
   * @param pc the {@link ProcessContext} to use.
   */
  void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc);

  /**
   * Performs the tool-specific installation of a single plugin.
   *
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   * @param pc the {@link ProcessContext} to use.
   * @return {@code true} if the installation of the plugin succeeded, {@code false} if not.
   */
  boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc);

  /**
   * Ensures that the tool itself is installed and then installs the plugin.
   *
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   */
  void installPlugin(ToolPluginDescriptor plugin, final Step step);

  /**
   * @param plugin the {@link ToolPluginDescriptor} to uninstall.
   */
  void uninstallPlugin(ToolPluginDescriptor plugin);

  /**
   * Uninstalls all currently installed plugins so that they can be installed again as configured in the project settings.
   */
  void deleteAllPlugins();

  /**
   * @param plugin the in {@link ToolPluginDescriptor#active() active} {@link ToolPluginDescriptor} that is skipped for regular plugin installation.
   */
  void handleInstallForInactivePlugin(ToolPluginDescriptor plugin);

  /**
   * @param plugin the {@link ToolPluginDescriptor plugin} to search for.
   * @return the {@link Path} to the plugin marker file or {@code null} if we are not inside an IDEasy project.
   */
  Path retrievePluginMarkerFilePath(ToolPluginDescriptor plugin);

  /**
   * Creates a marker file for a plugin in {@code $IDE_HOME/.ide/plugin.<<tool>>.<<edition>>.<<plugin-name>>}.
   *
   * @param plugin the plugin the {@link ToolPluginDescriptor plugin} for which the marker file should be created.
   */
  void createPluginMarkerFile(ToolPluginDescriptor plugin);
}
