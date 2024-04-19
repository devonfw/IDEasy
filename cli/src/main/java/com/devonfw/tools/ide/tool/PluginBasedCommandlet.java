package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.ide.PluginDescriptorImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for {@link LocalToolCommandlet}s that support plugins. It can automatically install configured plugins for
 * the tool managed by this commandlet.
 */
public abstract class PluginBasedCommandlet extends LocalToolCommandlet {

  private Map<String, PluginDescriptor> pluginsMapById;
  private Map<String, PluginDescriptor> pluginsMapByName;

  private Collection<PluginDescriptor> configuredPlugins;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   * method.
   */
  public PluginBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  protected PluginMaps getPluginsMap() {

    if (this.pluginsMapById == null && this.pluginsMapByName == null) {
      Map<String, PluginDescriptor> mapById = new HashMap<>();
      Map<String, PluginDescriptor> mapByName = new HashMap<>();

      // Load project-specific plugins
      Path pluginsPath = getPluginsConfigPath();
      loadPluginsFromDirectory(mapById, mapByName, pluginsPath);

      // Load user-specific plugins, this is done after loading the project-specific plugins so the user can potentially
      // override plugins (e.g. change active flag).
      Path userPluginsPath = getUserHomePluginsConfigPath();
      loadPluginsFromDirectory(mapById, mapByName, userPluginsPath);

      this.pluginsMapById = mapById;
      this.pluginsMapByName = mapByName;
    }

    return new PluginMaps(this.pluginsMapById, this.pluginsMapByName);
  }

  /**
   * Loads plugin descriptors from a directory into two maps: one keyed by plugin ID and the other keyed by plugin name.
   *
   * @param mapById   A map with plugin IDs as keys and corresponding plugin descriptors as values.
   * @param mapByName A map with plugin names as keys and corresponding plugin descriptors as values.
   * @param pluginsPath The path to the directory containing plugin configuration files.
   */
  private void loadPluginsFromDirectory(Map<String, PluginDescriptor> mapById, Map<String, PluginDescriptor> mapByName, Path pluginsPath) {

    this.context.getFileAccess()
        .listChildren(pluginsPath, p -> p.getFileName().toString().endsWith(IdeContext.EXT_PROPERTIES))
        .forEach(child -> {
          PluginDescriptor descriptor = PluginDescriptorImpl.of(child, this.context, isPluginUrlNeeded());
          if (!descriptor.getId().isEmpty()) {
            PluginDescriptor duplicateById = mapById.put(descriptor.getId(), descriptor);
            if (duplicateById != null)
              this.context.info("Plugin {} from project is overridden by {}", descriptor.getId(), child);
          }
          if (!descriptor.getName().isEmpty()) {
            PluginDescriptor duplicateByName = mapByName.put(descriptor.getName(), descriptor);
            if (duplicateByName != null)
              this.context.info("Plugin {} from project is overridden by {}", descriptor.getName(), child);
          }
        });
  }

  /**
   * @return {@code true} if {@link PluginDescriptor#getUrl() plugin URL} property is needed, {@code false} otherwise.
   */
  protected boolean isPluginUrlNeeded() {

    return false;
  }

  /**
   * @return the {@link Path} to the folder with the plugin configuration files inside the settings.
   */
  protected Path getPluginsConfigPath() {

    return this.context.getSettingsPath().resolve(this.tool).resolve(IdeContext.FOLDER_PLUGINS);
  }

  private Path getUserHomePluginsConfigPath() {

    return this.context.getUserHomeIde().resolve("settings").resolve(this.tool).resolve(IdeContext.FOLDER_PLUGINS);
  }

  /**
   * @return the immutable {@link Collection} of {@link PluginDescriptor}s configured for this IDE tool.
   */
  public Collection<PluginDescriptor> getConfiguredPlugins() {

    if (this.configuredPlugins == null) {
      this.configuredPlugins = Collections.unmodifiableCollection(getPluginsMap().getById().values());
    }
    return this.configuredPlugins;
  }

  /**
   * @return the {@link Path} where the plugins of this {@link IdeToolCommandlet} shall be installed.
   */
  public Path getPluginsInstallationPath() {

    return this.context.getPluginsPath().resolve(this.tool);
  }

  /**
   * @param plugin the {@link PluginDescriptor} to install.
   */
  public abstract void installPlugin(PluginDescriptor plugin);

  /**
   * @param plugin the {@link PluginDescriptor} to uninstall.
   */
  public void uninstallPlugin(PluginDescriptor plugin) {

    Path pluginsPath = getPluginsInstallationPath();
    if (!Files.isDirectory(pluginsPath)) {
      this.context.debug("Omitting to uninstall plugin {} ({}) as plugins folder does not exist at {}",
          plugin.getName(), plugin.getId(), pluginsPath);
      return;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    Path match = fileAccess.findFirst(pluginsPath, p -> p.getFileName().toString().startsWith(plugin.getId()), false);
    if (match == null) {
      this.context.debug("Omitting to uninstall plugin {} ({}) as plugins folder does not contain a match at {}",
          plugin.getName(), plugin.getId(), pluginsPath);
      return;
    }
    fileAccess.delete(match);
  }

  /**
   * Retrieves the plugin descriptor for the specified key, which can be either the ID or filename of the plugin properties file.
   *
   * @param key The key representing the plugin, which can be either the ID or filename
   * (excluding the ".properties" extension) of the plugin configuration file.
   * @return the {@link PluginDescriptor} for the given {@code key}.
   */
  public PluginDescriptor getPlugin(String key) {

    if (key == null) {
      return null;
    }
    if (key.endsWith(IdeContext.EXT_PROPERTIES)) {
      key = key.substring(0, key.length() - IdeContext.EXT_PROPERTIES.length());
    }

    PluginDescriptor pluginDescriptor = getPluginsMap().getById().get(key);

    if (pluginDescriptor == null) {
      pluginDescriptor = getPluginsMap().getByName().get(key);
    }

    if (pluginDescriptor == null) {
      throw new CliException(
          "Could not find plugin " + key + " at " + getPluginsConfigPath().resolve(key) + ".properties");
    }
    return pluginDescriptor;
  }

  /**
   * Iterates over plugins from both maps by name and ID, installing missing plugins and handling inactive ones.
   */
  @Override
  protected boolean doInstall(boolean silent) {

    boolean newlyInstalled = super.doInstall(silent);
    // post installation...
    boolean installPlugins = newlyInstalled;
    Path pluginsInstallationPath = getPluginsInstallationPath();
    if (newlyInstalled) {
      this.context.getFileAccess().delete(pluginsInstallationPath);
    } else if (!Files.isDirectory(pluginsInstallationPath)) {
      installPlugins = true;
    }
    if (installPlugins) {
      Map<String, PluginDescriptor> mapById = getPluginsMap().getById();
      Map<String, PluginDescriptor> mapByName = getPluginsMap().getByName();

      for (PluginDescriptor plugin : mapByName.values()) {
        if (!mapById.containsKey(plugin.getId())) {
          if (plugin.isActive()) {
            installPlugin(plugin);
          } else {
            handleInstall4InactivePlugin(plugin);
          }
        }
      }
      for (PluginDescriptor plugin : mapById.values()) {
        if (plugin.isActive()) {
          installPlugin(plugin);
        } else {
          handleInstall4InactivePlugin(plugin);
        }
      }
    }
    return newlyInstalled;
  }

  /**
   * @param plugin the in{@link PluginDescriptor#isActive() active} {@link PluginDescriptor} that is skipped for regular
   * plugin installation.
   */
  protected void handleInstall4InactivePlugin(PluginDescriptor plugin) {

    this.context.debug("Omitting installation of inactive plugin {} ({}).", plugin.getName(), plugin.getId());
  }
}
