package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

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

  @Override
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
