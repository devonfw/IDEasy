package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

/**
 * {@link Commandlet} for tools that support plugin management.
 * <p>
 * This follows the same pattern as {@link com.devonfw.tools.ide.tool.ide.IdeToolCommandlet}, decoupling plugin capabilities from the installation mechanism.
 * Both binary-installed IDEs (VS Code, IntelliJ) and package-manager-installed tools (Spyder via pip) can support plugins by composing a
 * {@link PluginManager}.
 * </p>
 */
public interface PluginBasedCommandlet extends LocalToolCommandlet {

  /** The zero-based index of the major version segment (e.g. {@code 1} in {@code 1.90.0}). */
  int MAJOR_SEGMENT = 0;

  /** The zero-based index of the minor version segment (e.g. {@code 90} in {@code 1.90.0}). */
  int MINOR_SEGMENT = 1;

  /** The zero-based index of the fix version segment (e.g. {@code 0} in {@code 1.90.0}). */
  int FIX_SEGMENT = 2;

  /**
   * @return the {@link PluginManager} implementing the plugin logic of the tool. Needed so that the default methods of this interface can delegate the shared
   *     plugin behaviour to a single implementation.
   */
  PluginManager getPluginManager();

  /**
   * Determines whether all installed plugins of this tool have to be purged (reset and reinstalled) as part of the installation. A purge is required if any
   * of the {@link #getIncompatibleVersionSegments() incompatible version segments} between the installed and the requested version differs, if the tool edition
   * changed, or if the tool is installed for the first time. Concrete IDEs may override this to apply their own compatibility strategy.
   *
   * @param request the {@link ToolInstallRequest} carrying the {@link ToolInstallRequest#getInstalled() installed} and
   *     {@link ToolInstallRequest#getRequested() requested} edition and version.
   * @return {@code true} if the installed plugins are considered incompatible with the requested version and have to be purged, {@code false} otherwise.
   */
  default boolean isPluginPurgeRequired(ToolInstallRequest request) {

    return getPluginManager().isPluginPurgeRequired(request);
  }

  /**
   * @return the set of version segment indices (see {@link #MAJOR_SEGMENT}, {@link #MINOR_SEGMENT}, and {@link #FIX_SEGMENT}) at which a change is considered
   *     incompatible and hence requires a plugin purge. By default only a change of the {@link #MAJOR_SEGMENT major} segment triggers a purge, so a minor or
   *     fix update keeps the installed plugins. Concrete IDEs may override this to reflect their own versioning scheme.
   */
  default Set<Integer> getIncompatibleVersionSegments() {

    return Set.of(MAJOR_SEGMENT);
  }

  /**
   * @return the configured edition of the tool owning the plugins (see {@code AbstractToolCommandlet#getConfiguredEdition()}).
   */
  String getConfiguredEdition();

  /**
   * @return the installed edition of the tool owning the plugins or {@code null} if not installed (see {@code AbstractToolCommandlet#getInstalledEdition()}).
   */
  String getInstalledEdition();

  /**
   * @return the {@link Path} to the folder with the plugin configuration files inside the settings. The default implementation is shared by all plugin-capable
   *     tools (see {@link AbstractPluginBasedCommandlet} and {@link com.devonfw.tools.ide.tool.pip.PipBasedIdeToolCommandlet}).
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
