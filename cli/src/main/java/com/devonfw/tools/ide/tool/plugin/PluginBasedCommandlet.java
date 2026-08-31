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
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;

/**
 * Base class for {@link LocalToolCommandlet}s that support plugins. It can automatically install configured plugins for the tool managed by this commandlet.
 */
public abstract class PluginBasedCommandlet extends LocalToolCommandlet implements PluginFeatures {

  private static final Logger LOG = LoggerFactory.getLogger(PluginBasedCommandlet.class);

  private final PluginManager pluginManager;

  /** {@link FlagProperty} to force the reset and reinstallation of plugins as configured in the project settings. */
  public FlagProperty forcePluginReinstall;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public PluginBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
    this.pluginManager = new PluginManager(context, this);
  }

  @Override
  protected void initProperties() {

    this.forcePluginReinstall = add(new FlagProperty("--force-plugin-reinstall"));
    super.initProperties();
  }

  /**
   * @return the {@link PluginManager} for this tool.
   */
  protected PluginManager getPluginManager() {

    return this.pluginManager;
  }

  @Override
  public ToolPlugins getPlugins() {

    return this.pluginManager.getPlugins();
  }

  @Override
  public boolean isPluginUrlNeeded() {

    return false;
  }

  @Override
  public Path getPluginsConfigPath() {

    return this.context.getSettingsPath().resolve(this.tool).resolve(IdeContext.FOLDER_PLUGINS);
  }

  @Override
  protected void postInstall(ToolInstallRequest request) {

    super.postInstall(request);
    if (!request.isAlreadyInstalled() || this.forcePluginReinstall.isTrue()) {
      this.pluginManager.resetPlugins();
    }
    this.context.getFileAccess().mkdirs(getPluginsInstallationPath());
    installPlugins(getPlugins().getPlugins(), request.getProcessContext());
  }

  @Override
  public void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {

    this.pluginManager.installPlugins(plugins, pc);
  }

  /**
   * Method to install active plugins or to handle install for inactive plugins
   *
   * @param plugins as {@link Collection} of plugins to install.
   * @param pc the {@link ProcessContext} to use.
   */
  protected void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {

    Set<String> extraPlugins = getExtraPlugins(plugins);
    String edition = getConfiguredEdition();
    List<ToolPluginDescriptor> pluginsToInstall = new ArrayList<>(plugins.size());
    for (ToolPluginDescriptor plugin : plugins) {
      if (plugin.excludedEditions().contains(edition)) {
        LOG.debug("Skipping plugin '{}' (excluded for edition '{}').", plugin.name(), edition);
      } else if (plugin.active() || extraPlugins.contains(plugin.name())) {
        pluginsToInstall.add(plugin);
      } else {
        Path pluginMarkerFile = retrievePluginMarkerFilePath(plugin);
        if ((pluginMarkerFile == null) || !Files.exists(pluginMarkerFile)) {
          handleInstallForInactivePlugin(plugin);
        }
      }
    }
    int currentPluginIndex = 1;
    int totalPlugins = pluginsToInstall.size();
    for (ToolPluginDescriptor plugin : pluginsToInstall) {
      Path pluginMarkerFile = retrievePluginMarkerFilePath(plugin);
      boolean pluginMarkerFileExists = (pluginMarkerFile != null) && Files.exists(pluginMarkerFile);
      if (pluginMarkerFileExists) {
        LOG.debug("Markerfile for IDE {} and plugin '{}' already exists.", getName(), plugin.name());
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
  protected Set<String> getExtraPlugins(Collection<ToolPluginDescriptor> plugins) {

    String variable = EnvironmentVariables.getToolExtraPluginsVariable(this.tool);
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
          getPluginsConfigPath(), getUserHomePluginsConfigPath());
    }
    return extraPlugins;
  }

  private void doInstallPluginStep(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {
    boolean result = installPlugin(plugin, step, pc);
    if (result) {
      createPluginMarkerFile(plugin);
    }
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor plugin} to search for.
   * @return Path to the plugin marker file.
   */
  public Path retrievePluginMarkerFilePath(ToolPluginDescriptor plugin) {

    return this.pluginManager.retrievePluginMarkerFilePath(plugin);
  }

  @Override
  public void createPluginMarkerFile(ToolPluginDescriptor plugin) {

    this.pluginManager.createPluginMarkerFile(plugin);
  }

  @Override
  public Path getPluginsInstallationPath() {

    return this.context.getPluginsPath().resolve(this.tool);
  }

  @Override
  public void installPlugin(ToolPluginDescriptor plugin, final Step step) {

    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI);
    ToolInstallRequest request = new ToolInstallRequest(true);
    install(request);
    installPlugin(plugin, step, pc);
  }

  @Override
  public void uninstallPlugin(ToolPluginDescriptor plugin) {

    this.pluginManager.uninstallPlugin(plugin);
  }

  @Override
  public void deleteAllPlugins() {

    this.context.getFileAccess().delete(getPluginsInstallationPath());
  }

  @Override
  public ToolPluginDescriptor getPlugin(String key) {

    return this.pluginManager.getPlugin(key);
  }

  @Override
  public void handleInstallForInactivePlugin(ToolPluginDescriptor plugin) {

    LOG.debug("Omitting installation of inactive plugin {} ({}).", plugin.name(), plugin.id());
  }
}
