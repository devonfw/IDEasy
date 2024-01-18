package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.ide.PluginDescriptorImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public abstract class PluginBasedCommandlet extends LocalToolCommandlet {

  private Map<String, PluginDescriptor> pluginsMap;

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


  protected Map<String, PluginDescriptor> getPluginsMap() {

    if (this.pluginsMap == null) {
      Map<String, PluginDescriptor> map = new HashMap<>();

      // Load project-specific plugins
      Path pluginsPath = getPluginsConfigPath();
      loadPluginsFromDirectory(map, pluginsPath);

      // Load user-specific plugins
      Path userPluginsPath = getUserHomePluginsConfigPath();
      loadPluginsFromDirectory(map, userPluginsPath);

      this.pluginsMap = map;
    }

    return this.pluginsMap;
  }

  private void loadPluginsFromDirectory(Map<String, PluginDescriptor> map, Path pluginsPath) {
    if (Files.isDirectory(pluginsPath)) {
      try (Stream<Path> childStream = Files.list(pluginsPath)) {
        Iterator<Path> iterator = childStream.iterator();
        while (iterator.hasNext()) {
          Path child = iterator.next();
          String filename = child.getFileName().toString();
          if (filename.endsWith(IdeContext.EXT_PROPERTIES) && Files.exists(child)) {
            PluginDescriptor descriptor = PluginDescriptorImpl.of(child, this.context, isPluginUrlNeeded());

            // Priority to user-specific plugins
            map.put(descriptor.getName(), descriptor);
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to list children of directory " + pluginsPath, e);
      }
    }
  }

  /**
   * @return {@code true} if {@link PluginDescriptor#getUrl() plugin URL} property is needed, {@code false} otherwise.
   */
  protected boolean isPluginUrlNeeded() {

    return false;
  }

  protected Path getPluginsConfigPath() {

    return this.context.getSettingsPath().resolve(this.tool).resolve(IdeContext.FOLDER_PLUGINS);
  }

  private Path getUserHomePluginsConfigPath() {

    return context.getUserHome().resolve(Paths.get(".ide", "settings", this.tool, IdeContext.FOLDER_PLUGINS));
  }


  /**
   * @return the immutable {@link Collection} of {@link PluginDescriptor}s configured for this IDE tool.
   */
  public Collection<PluginDescriptor> getConfiguredPlugins() {

    if (this.configuredPlugins == null) {
      this.configuredPlugins = Collections.unmodifiableCollection(getPluginsMap().values());
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
   * @param key the filename of the properties file configuring the requested plugin (typically excluding the
   *        ".properties" extension).
   * @return the {@link PluginDescriptor} for the given {@code key}.
   */
  public PluginDescriptor getPlugin(String key) {

    if (key == null) {
      return null;
    }
    if (key.endsWith(IdeContext.EXT_PROPERTIES)) {
      key = key.substring(0, key.length() - IdeContext.EXT_PROPERTIES.length());
    }
    PluginDescriptor pluginDescriptor = getPluginsMap().get(key);
    if (pluginDescriptor == null) {
      throw new CliException(
          "Could not find plugin " + key + " at " + getPluginsConfigPath().resolve(key) + ".properties");
    }
    return pluginDescriptor;
  }
}
