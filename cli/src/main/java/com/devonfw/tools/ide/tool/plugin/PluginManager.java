package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;

/**
 * Manages plugin configuration and common plugin operations for tools that support plugins.
 */
public class PluginManager {

  private static final Logger LOG = LoggerFactory.getLogger(PluginManager.class);

  /** The prefix of a plugin marker file inside {@link IdeContext#FOLDER_DOT_IDE .ide} Folder. */
  public static final String MARKER_FILE_PREFIX = "plugin.";

  /** The infix of a plugin marker file separating the plugin name from its version. */
  public static final String MARKER_FILE_VERSION_INFIX = ".version-";

  private final IdeContext context;

  private final PluginFeatures tool;

  private ToolPlugins plugins;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link PluginFeatures tool} owning the plugins managed by this {@link PluginManager}.
   */
  public PluginManager(IdeContext context, PluginFeatures tool) {
    super();
    this.context = context;
    this.tool = tool;
  }


  /**
   * @return the {@link ToolPlugins} of this {@link PluginBasedCommandlet}.
   */
  public ToolPlugins getPlugins() {

    if (this.plugins == null) {
      ToolPlugins toolPlugins = new ToolPlugins();

      // Load project-specific plugins
      Path pluginsPath = this.tool.getPluginsConfigPath();
      loadPluginsFromDirectory(toolPlugins, pluginsPath);

      // Load user-specific plugins, this is done after loading the project-specific plugins so the user can potentially
      // override plugins (e.g. change active flag).
      Path userPluginsPath = getUserHomePluginConfigPath();
      loadPluginsFromDirectory(toolPlugins, userPluginsPath);

      this.plugins = toolPlugins;
    }

    return this.plugins;
  }


  private void loadPluginsFromDirectory(ToolPlugins map, Path pluginsPath) {

    List<Path> children = this.context.getFileAccess()
        .listChildren(pluginsPath, p -> p.getFileName().toString().endsWith(IdeContext.EXT_PROPERTIES));
    for (Path child : children) {
      ToolPluginDescriptor descriptor = ToolPluginDescriptor.of(child, this.context, this.tool.isPluginUrlNeeded());
      map.add(descriptor);
    }
  }

  private Path getUserHomePluginConfigPath() {
    return this.context.getUserHomeIde().resolve(IdeContext.FOLDER_SETTINGS).resolve(this.tool.getName()).resolve(IdeContext.FOLDER_PLUGINS);
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
          "Could not find plugin " + key + " at " + this.tool.getPluginsConfigPath().resolve(key) + ".properties");
    }
    return pluginDescriptor;
  }

  /**
   * Reset all installed plugins by deleting the {@link PluginFeatures#getPluginsInstallationPath() plugins installation folder} and all plugin marker files.
   */
  public void resetPlugins() {
    LOG.info("Resetting all installed plugins...");
    this.tool.deleteAllPlugins();
    deleteAllPluginMarkerFiles();
  }

  /**
   * Deletes all plugin marker files the {@link PluginFeatures tool} so that its plugins will be installed again.
   */
  public void deleteAllPluginMarkerFiles() {
    FileAccess fileAccess = this.context.getFileAccess();
    List<Path> markerFiles = fileAccess.listChildren(this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE), Files::isRegularFile);
    for (Path path : markerFiles) {
      if (path.getFileName().toString().startsWith(MARKER_FILE_PREFIX + this.tool.getName())) {
        fileAccess.delete(path);
        LOG.debug("Plugin marker file {} got deleted.", path);
      }
    }
  }

  /**
   * Installs the given plugins, activating those explicitly requested via the tool-specific {@code «TOOL»_EXTRA_PLUGINS} variable in addition to the
   * {@link ToolPluginDescriptor#active() active} ones, and handles the inactive plugins that are not explicitly requested.
   *
   * @param plugins as {@link Collection} of plugins to install.
   * @param pc the {@link ProcessContext} to use.
   */
  public void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {

    Set<String> extraPlugins = getExtraPlugins(plugins);
    String edition = this.tool.getConfiguredEdition();
    List<ToolPluginDescriptor> pluginsToInstall = new ArrayList<>(plugins.size());
    for (ToolPluginDescriptor plugin : plugins) {
      if (plugin.excludedEditions().contains(edition)) {
        LOG.debug("Skipping plugin '{}' (excluded for edition '{}').", plugin.name(), edition);
      } else if (plugin.active() || extraPlugins.contains(plugin.name())) {
        pluginsToInstall.add(plugin);
      } else {
        Path pluginMarkerFile = retrievePluginMarkerFilePath(plugin);
        if ((pluginMarkerFile == null) || !Files.exists(pluginMarkerFile)) {
          this.tool.handleInstallForInactivePlugin(plugin);
        }
      }
    }
    int currentPluginIndex = 1;
    int totalPlugins = pluginsToInstall.size();
    for (ToolPluginDescriptor plugin : pluginsToInstall) {
      Path pluginMarkerFile = retrievePluginMarkerFilePath(plugin);
      boolean pluginMarkerFileExists = (pluginMarkerFile != null) && Files.exists(pluginMarkerFile);
      if (pluginMarkerFileExists) {
        LOG.debug("Markerfile for IDE {} and plugin '{}' already exists.", this.tool.getName(), plugin.name());
      }
      if (this.context.isForcePlugins() || !pluginMarkerFileExists) {
        String progressMarker = " (" + currentPluginIndex + "/" + totalPlugins + ")";
        Step step = this.context.newStep("Install plugin " + plugin.name() + progressMarker);
        step.run(() -> doInstallPluginStep(plugin, step, pc));
      } else {
        LOG.debug("Skipping installation of plugin '{}' due to existing marker file: {}", plugin.name(), pluginMarkerFile);
      }
      currentPluginIndex++;
    }
  }

  /**
   * @param plugins the configured {@link ToolPluginDescriptor plugins} used to detect undefined entries.
   * @return the {@link Set} of {@link ToolPluginDescriptor#name() plugin names} configured in the tool-specific {@code «TOOL»_EXTRA_PLUGINS} variable (e.g.
   *     {@code VSCODE_EXTRA_PLUGINS=copilot,docker}). This allows a user to permanently opt-in to plugins that are not {@link ToolPluginDescriptor#active()
   *     active} in the project settings, without modifying the shared settings and without losing them when plugins are purged and reinstalled on IDE upgrade.
   *     Values refer to the {@link ToolPluginDescriptor#name() name} of the plugin (the filename of its {@code .properties} file) and not to the
   *     {@link ToolPluginDescriptor#id() id}. Names that do not resolve to a configured plugin are logged as a warning and skipped so that a single stale entry
   *     cannot break the entire installation.
   */
  public Set<String> getExtraPlugins(Collection<ToolPluginDescriptor> plugins) {

    String variable = EnvironmentVariables.getToolExtraPluginsVariable(this.tool.getName());
    String value = this.context.getVariables().get(variable);
    if ((value == null) || value.isBlank()) {
      return Set.of();
    }
    Set<String> extraPlugins = new LinkedHashSet<>();
    for (String name : VariableLine.parseArray(value)) {
      if (name.endsWith(IdeContext.EXT_PROPERTIES)) {
        name = name.substring(0, name.length() - IdeContext.EXT_PROPERTIES.length());
      }
      extraPlugins.add(name);
    }
    Set<String> undefinedPlugins = new LinkedHashSet<>(extraPlugins);
    for (ToolPluginDescriptor plugin : plugins) {
      undefinedPlugins.remove(plugin.name());
    }
    for (String name : undefinedPlugins) {
      LOG.info("Ignoring undefined plugin '{}' configured in variable {} - no file {}{} found in {} or {}.", name, variable, name, IdeContext.EXT_PROPERTIES,
          this.tool.getPluginsConfigPath(), getUserHomePluginConfigPath());
    }
    return extraPlugins;
  }

  private void doInstallPluginStep(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {

    boolean result = this.tool.installPlugin(plugin, step, pc);
    if (result) {
      createPluginMarkerFile(plugin);
    }
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor plugin} to search for.
   * @return Path to the plugin marker file.
   */
  public Path retrievePluginMarkerFilePath(ToolPluginDescriptor plugin) {
    if (this.context.getIdeHome() != null) {
      String markerFileName = getMarkerFilePrefix(plugin);
      String version = plugin.version();
      if ((version != null) && !version.isBlank()) {
        markerFileName = markerFileName + ".version-" + normalizeMarkerFileSegment(version);
      }
      return this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(markerFileName);
    }
    return null;
  }

  private String getMarkerFilePrefix(ToolPluginDescriptor plugin) {
    return MARKER_FILE_PREFIX + this.tool.getName() + "." + this.tool.getInstalledEdition() + "." + plugin.name();
  }

  private String normalizeMarkerFileSegment(String value) {
    return value.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  /**
   * Creates a marker file for a plugin in $IDE_HOME/.ide/plugin.«ide».«plugin-name»
   *
   * @param plugin the {@link ToolPluginDescriptor plugin} for which the marker file should be created.
   */
  public void createPluginMarkerFile(ToolPluginDescriptor plugin) {
    Path pluginMarkerFilePath = retrievePluginMarkerFilePath(plugin);
    if (pluginMarkerFilePath != null) {
      FileAccess fileAccess = this.context.getFileAccess();
      fileAccess.mkdirs(pluginMarkerFilePath.getParent());
      deleteExistingPluginMarkerFiles(fileAccess, plugin, pluginMarkerFilePath);
      fileAccess.touch(pluginMarkerFilePath);
    }
  }

  private void deleteExistingPluginMarkerFiles(FileAccess fileAccess, ToolPluginDescriptor plugin, Path currentMarkerFilePath) {

    String markerFilePrefix = getMarkerFilePrefix(plugin);
    List<Path> markerFiles = fileAccess.listChildren(currentMarkerFilePath.getParent(),
        p -> {
          String fileName = p.getFileName().toString();
          return Files.isRegularFile(p) && (fileName.equals(markerFilePrefix) || fileName.startsWith(markerFilePrefix + MARKER_FILE_VERSION_INFIX));
        });
    for (Path markerFile : markerFiles) {
      if (!markerFile.equals(currentMarkerFilePath)) {
        fileAccess.delete(markerFile);
        LOG.debug("Deleted stale plugin marker file {} before creating {}.", markerFile, currentMarkerFilePath);
      }
    }
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor} to uninstall.
   */
  public void uninstallPlugin(ToolPluginDescriptor plugin) {

    boolean error = false;
    Path pluginsPath = this.tool.getPluginsInstallationPath();
    if (!Files.isDirectory(pluginsPath)) {
      LOG.debug("Omitting to uninstall plugin {} ({}) as plugins folder does not exist at {}",
          plugin.name(), plugin.id(), pluginsPath);
      error = true;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    Path match = fileAccess.findFirst(pluginsPath, p -> p.getFileName().toString().startsWith(plugin.id()), false);
    if (match == null) {
      LOG.debug("Omitting to uninstall plugin {} ({}) as plugins folder does not contain a match at {}",
          plugin.name(), plugin.id(), pluginsPath);
      error = true;
    }
    if (error) {
      LOG.error("Could not uninstall plugin {} because we could not find an installation", plugin);
    } else {
      fileAccess.delete(match);
      LOG.info("Successfully uninstalled plugin {}", plugin);
    }
  }
}
