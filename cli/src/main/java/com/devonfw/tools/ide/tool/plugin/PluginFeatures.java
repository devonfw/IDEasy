package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Path;
import java.util.Collection;

import com.devonfw.tools.ide.context.IdeContext;
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
   * @return the {@link IdeContext} of the tool owning the plugins. Needed so that the default methods of this interface can access the context.
   */
  IdeContext getContext();

  /**
   * @return the {@link PluginManager} implementing the plugin logic of the tool. Needed so that the default methods of this interface can delegate the shared
   *     plugin behaviour to a single implementation.
   */
  PluginManager getPluginManager();

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
   * @return the {@link Path} to the folder with the plugin configuration files inside the settings. The default implementation is shared by all plugin-capable
   *     tools (see {@link com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet} and {@link com.devonfw.tools.ide.tool.pip.PipBasedIdeToolCommandlet}).
   */
  default Path getPluginsConfigPath() {

    return getContext().getSettingsPath().resolve(getName()).resolve(IdeContext.FOLDER_PLUGINS);
  }

  /**
   * @return the {@link Path} where the plugins of this tool shall be installed.
   */
  Path getPluginsInstallationPath();

  /**
   * @return {@code true} if the {@link ToolPluginDescriptor#url() plugin url} is needed, {@code false} otherwise. The default (no URL) is shared by all
   *     plugin-capable tools; IDEs that need a URL (e.g. Eclipse) override this.
   */
  default boolean isPluginUrlNeeded() {

    return false;
  }

  /**
   * @return the {@link ToolPlugins} configured for this tool. The default implementation is shared by all plugin-capable tools.
   */
  default ToolPlugins getPlugins() {

    return getPluginManager().getPlugins();
  }

  /**
   * @param key the filename of the properties file configuring the requested plugin (typically excluding the ".properties" extension).
   * @return the {@link ToolPluginDescriptor} for the given {@code key}. The default implementation is shared by all plugin-capable tools.
   */
  default ToolPluginDescriptor getPlugin(String key) {

    return getPluginManager().getPlugin(key);
  }

  /**
   * Installs the given active plugins and handles the inactive ones. The default implementation is shared by all plugin-capable tools.
   *
   * @param plugins the {@link Collection} of {@link ToolPluginDescriptor plugins} to install.
   * @param pc the {@link ProcessContext} to use.
   */
  default void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {

    getPluginManager().installPlugins(plugins, pc);
  }

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
   * @param plugin the {@link ToolPluginDescriptor plugin} to search for.
   * @return the {@link Path} to the plugin marker file or {@code null} if we are not inside an IDEasy project. The default implementation is shared by all
   *     plugin-capable tools.
   */
  default Path retrievePluginMarkerFilePath(ToolPluginDescriptor plugin) {

    return getPluginManager().retrievePluginMarkerFilePath(plugin);
  }

  /**
   * Creates a marker file for a plugin in {@code $IDE_HOME/.ide/plugin.<<tool>>.<<edition>>.<<plugin-name>>}. The default implementation is shared by all
   * plugin-capable tools.
   *
   * @param plugin the plugin the {@link ToolPluginDescriptor plugin} for which the marker file should be created.
   */
  default void createPluginMarkerFile(ToolPluginDescriptor plugin) {

    getPluginManager().createPluginMarkerFile(plugin);
  }
}
