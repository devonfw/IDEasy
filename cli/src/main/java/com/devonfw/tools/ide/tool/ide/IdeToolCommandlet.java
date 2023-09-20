package com.devonfw.tools.ide.tool.ide;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for an IDE (integrated development environment).
 */
public abstract class IdeToolCommandlet extends ToolCommandlet {

  private Map<String, PluginDescriptor> pluginsMap;

  private Collection<PluginDescriptor> configuredPlugins;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   *        method.
   */
  public IdeToolCommandlet(IdeContext context, String tool, Set<String> tags) {

    super(context, tool, tags);
    assert (tags.contains(TAG_IDE));
  }

  private Map<String, PluginDescriptor> getPluginsMap() {

    if (this.pluginsMap == null) {
      Map<String, PluginDescriptor> map = new HashMap<>();
      Path pluginsPath = getPluginsConfigPath();
      if (Files.isDirectory(pluginsPath)) {
        try (Stream<Path> childStream = Files.list(pluginsPath)) {
          Iterator<Path> iterator = childStream.iterator();
          while (iterator.hasNext()) {
            Path child = iterator.next();
            String filename = child.getFileName().toString();
            if (filename.endsWith(IdeContext.EXT_PROPERTIES) && Files.exists(child)) {
              PluginDescriptor descriptor = PluginDescriptorImpl.of(child, this.context, isPluginUrlNeeded());
              map.put(descriptor.getName(), descriptor);
            }
          }
        } catch (IOException e) {
          throw new IllegalStateException("Failed to list children of directory " + pluginsPath, e);
        }
      }
      this.pluginsMap = map;
    }
    return this.pluginsMap;
  }

  private Path getPluginsConfigPath() {

    return this.context.getSettingsPath().resolve(this.tool).resolve(IdeContext.FOLDER_PLUGINS);
  }

  /**
   * @return {@code true} if {@link PluginDescriptor#getUrl() plugin URL} property is needed, {@code false} otherwise.
   */
  protected boolean isPluginUrlNeeded() {

    return false;
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

    // TODO add edition???
    return this.context.getPluginsPath().resolve(this.tool);
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
      for (PluginDescriptor plugin : getPluginsMap().values()) {
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
   *        plugin installation.
   */
  protected void handleInstall4InactivePlugin(PluginDescriptor plugin) {

    this.context.debug("Omitting installation of inactive plugin {} ({}).", plugin.getName(), plugin.getId());
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

}
