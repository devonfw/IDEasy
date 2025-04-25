package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;

/**
 * Base class for {@link LocalToolCommandlet}s that support plugins. It can automatically install configured plugins for the tool managed by this commandlet.
 */
public abstract class PluginBasedCommandlet extends LocalToolCommandlet {

  private ToolPlugins plugins;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public PluginBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  public ToolPlugins getPlugins() {

    if (this.plugins == null) {
      ToolPlugins toolPlugins = new ToolPlugins(this.context);

      // Load project-specific plugins
      Path pluginsPath = getPluginsConfigPath();
      loadPluginsFromDirectory(toolPlugins, pluginsPath);

      // Load user-specific plugins, this is done after loading the project-specific plugins so the user can potentially
      // override plugins (e.g. change active flag).
      Path userPluginsPath = getUserHomePluginsConfigPath();
      loadPluginsFromDirectory(toolPlugins, userPluginsPath);

      this.plugins = toolPlugins;
    }

    return this.plugins;
  }

  private void loadPluginsFromDirectory(ToolPlugins map, Path pluginsPath) {

    List<Path> children = this.context.getFileAccess()
        .listChildren(pluginsPath, p -> p.getFileName().toString().endsWith(IdeContext.EXT_PROPERTIES));
    for (Path child : children) {
      ToolPluginDescriptor descriptor = ToolPluginDescriptor.of(child, this.context, isPluginUrlNeeded());
      map.add(descriptor);
    }
  }

  /**
   * @return {@code true} if {@link ToolPluginDescriptor#url() plugin URL} property is needed, {@code false} otherwise.
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
   * @return the {@link Path} where the plugins of this {@link IdeToolCommandlet} shall be installed.
   */
  public Path getPluginsInstallationPath() {

    return this.context.getPluginsPath().resolve(this.tool);
  }

  @Override
  protected void postInstall(boolean newlyInstalled, ProcessContext pc) {

    super.postInstall(newlyInstalled, pc);
    Path pluginsInstallationPath = getPluginsInstallationPath();
    FileAccess fileAccess = this.context.getFileAccess();
    if (newlyInstalled) {
      fileAccess.delete(pluginsInstallationPath);
      List<Path> markerFiles = fileAccess.listChildren(this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE), Files::isRegularFile);
      for (Path path : markerFiles) {
        if (path.getFileName().toString().startsWith("plugin." + getName())) {
          this.context.debug("Plugin marker file {} got deleted.", path);
          fileAccess.delete(path);
        }
      }
    }
    fileAccess.mkdirs(pluginsInstallationPath);
    installPlugins(pc);
  }

  private void installPlugins(ProcessContext pc) {
    installPlugins(getPlugins().getPlugins(), pc);
  }

  /**
   * Method to install active plugins or to handle install for inactive plugins
   *
   * @param plugins as {@link Collection} of plugins to install.
   * @param pc the {@link ProcessContext} to use.
   */
  protected void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {
    for (ToolPluginDescriptor plugin : plugins) {
      if (plugin.active()) {
        if (!this.context.isForcePlugins() && retrievePluginMarkerFilePath(plugin) != null && Files.exists(retrievePluginMarkerFilePath(plugin))) {
          this.context.debug("Markerfile for IDE: {} and active plugin: {} already exists.", getName(), plugin.name());
        } else {
          try (Step step = this.context.newStep("Install plugin " + plugin.name())) {
            boolean result = installPlugin(plugin, step, pc);
            if (result) {
              createPluginMarkerFile(plugin);
            }
          }
        }
      } else {
        if (retrievePluginMarkerFilePath(plugin) != null && Files.exists(retrievePluginMarkerFilePath(plugin))) {
          this.context.debug("Markerfile for IDE: {} and inactive plugin: {} already exists.", getName(), plugin.name());
        } else {
          handleInstallForInactivePlugin(plugin);
        }
      }
    }
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor plugin} to search for.
   * @return Path to the plugin marker file.
   */
  public Path retrievePluginMarkerFilePath(ToolPluginDescriptor plugin) {
    if (this.context.getIdeHome() != null) {
      return this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE)
          .resolve("plugin" + "." + getName() + "." + getInstalledEdition() + "." + plugin.name());
    }
    return null;
  }

  /**
   * Creates a marker file for a plugin in $IDE_HOME/.ide/plugin.«ide».«plugin-name»
   *
   * @param plugin the {@link ToolPluginDescriptor plugin} for which the marker file should be created.
   */
  public void createPluginMarkerFile(ToolPluginDescriptor plugin) {
    if (this.context.getIdeHome() != null) {
      Path hiddenIdePath = this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE);
      this.context.getFileAccess().mkdirs(hiddenIdePath);
      this.context.getFileAccess().touch(hiddenIdePath.resolve("plugin" + "." + getName() + "." + getInstalledEdition() + "." + plugin.name()));
    }
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   * @param pc the {@link ProcessContext} to use.
   * @return boolean true if the installation of the plugin succeeded, false if not.
   */
  public abstract boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc);

  /**
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   */
  public void installPlugin(ToolPluginDescriptor plugin, final Step step) {
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI);
    install(true, pc);
    installPlugin(plugin, step, pc);
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor} to uninstall.
   */
  public void uninstallPlugin(ToolPluginDescriptor plugin) {

    boolean error = false;
    Path pluginsPath = getPluginsInstallationPath();
    if (!Files.isDirectory(pluginsPath)) {
      this.context.debug("Omitting to uninstall plugin {} ({}) as plugins folder does not exist at {}",
          plugin.name(), plugin.id(), pluginsPath);
      error = true;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    Path match = fileAccess.findFirst(pluginsPath, p -> p.getFileName().toString().startsWith(plugin.id()), false);
    if (match == null) {
      this.context.debug("Omitting to uninstall plugin {} ({}) as plugins folder does not contain a match at {}",
          plugin.name(), plugin.id(), pluginsPath);
      error = true;
    }
    if (error) {
      context.error("Could not uninstall plugin " + plugin + " because we could not find an installation");
    } else {
      fileAccess.delete(match);
      context.info("Successfully uninstalled plugin " + plugin);
    }
  }

  /**
   * @param key the filename of the properties file configuring the requested plugin (typically excluding the ".properties" extension).
   * @return the {@link ToolPluginDescriptor} for the given {@code key}.
   */
  public ToolPluginDescriptor getPlugin(String key) {

    if (key == null) {
      return null;
    }
    if (key.endsWith(IdeContext.EXT_PROPERTIES)) {
      key = key.substring(0, key.length() - IdeContext.EXT_PROPERTIES.length());
    }

    ToolPlugins toolPlugins = getPlugins();
    ToolPluginDescriptor pluginDescriptor = toolPlugins.getByName(key);
    if (pluginDescriptor == null) {
      throw new CliException(
          "Could not find plugin " + key + " at " + getPluginsConfigPath().resolve(key) + ".properties");
    }
    return pluginDescriptor;
  }

  /**
   * @param plugin the in{@link ToolPluginDescriptor#active() active} {@link ToolPluginDescriptor} that is skipped for regular plugin installation.
   */
  protected void handleInstallForInactivePlugin(ToolPluginDescriptor plugin) {

    this.context.debug("Omitting installation of inactive plugin {} ({}).", plugin.name(), plugin.id());
  }
}
