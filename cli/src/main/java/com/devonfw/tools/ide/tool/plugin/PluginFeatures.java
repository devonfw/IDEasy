package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Path;

import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;

/**
 * Interface for tools that support plugin management.
 * <p>
 * This follows the same pattern as {@link com.devonfw.tools.ide.tool.ide.IdeFeatures}, decoupling plugin capabilities
 * from the installation mechanism. Both binary-installed IDEs (VS Code, IntelliJ) and package-manager-installed tools
 * (Spyder via pip) can support plugins by composing a {@link PluginManager}.
 * </p>
 */
public interface PluginFeatures {

  /**
   * @return the {@link ToolPlugins} configured for this tool.
   */
  ToolPlugins getPlugins();

  /**
   * Installs a single plugin.
   *
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   * @param pc the {@link ProcessContext} to use.
   * @return {@code true} if the installation of the plugin succeeded, {@code false} if not.
   */
  boolean doInstallPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc);

  /**
   * Installs a plugin without a step (for programmatic installation).
   *
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   */
  void installPlugin(ToolPluginDescriptor plugin, final Step step);

  /**
   * Uninstalls a plugin.
   *
   * @param plugin the {@link ToolPluginDescriptor} to uninstall.
   */
  void uninstallPlugin(ToolPluginDescriptor plugin);

  /**
   * @param key the filename of the properties file configuring the requested plugin (typically excluding the
   *     ".properties" extension).
   * @return the {@link ToolPluginDescriptor} for the given {@code key}.
   */
  ToolPluginDescriptor getPlugin(String key);

  /**
   * @return the {@link Path} where the plugins of this tool shall be installed.
   */
  Path getPluginsInstallationPath();

  /**
   * @return the {@link PluginManager} for this tool.
   */
  PluginManager getPluginManager();
}
