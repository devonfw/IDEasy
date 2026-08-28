package com.devonfw.tools.ide.tool.pip;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ide.IdeFeatures;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeWorkspaceConfigurer;
import com.devonfw.tools.ide.tool.plugin.PluginFeatures;
import com.devonfw.tools.ide.tool.plugin.PluginManager;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.tool.plugin.ToolPlugins;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Base class for pip-based IDE tools that should launch in the background instead of blocking the terminal. Implements {@link IdeFeatures} to provide IDE
 * workspace configuration capabilities and {@link PluginFeatures} for plugin management.
 */
public abstract class PipBasedIdeToolCommandlet extends PipBasedCommandlet implements IdeFeatures, PluginFeatures {

  private static final Logger LOG = LoggerFactory.getLogger(PipBasedIdeToolCommandlet.class);

  private final IdeWorkspaceConfigurer workspaceConfigurer;

  private final PluginManager pluginManager;

  /** {@link FlagProperty} to force the reset and reinstallation of plugins as configured in the project settings. */
  public FlagProperty forcePluginReinstall;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool.
   */
  public PipBasedIdeToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {
    super(context, tool, tags);
    this.workspaceConfigurer = new IdeWorkspaceConfigurer(context, tool);
    this.pluginManager = new PluginManager(context, this);
  }

  @Override
  protected void initProperties() {
    this.forcePluginReinstall = add(new FlagProperty("--force-plugin-reinstall"));
    super.initProperties();
  }

  @Override
  public ProcessResult runTool(List<String> args) {
    configureWorkspace();
    return runTool(ProcessMode.BACKGROUND, null, args);
  }

  @Override
  public void configureWorkspace() {
    this.workspaceConfigurer.configureWorkspace();
  }

  @Override
  public Path getIdeMetadataPath() {

    return this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(getName()).resolve(this.context.getWorkspaceName());
  }

  /**
   * Imports the repository specified by the given {@link Path} into the IDE managed by this commandlet.
   *
   * @param repositoryPath the {@link Path} to the repository directory to import.
   */
  @Override
  public void importRepository(Path repositoryPath) {
    throw new UnsupportedOperationException("Repository import is not yet implemented for IDE " + this.tool);
  }

  @Override
  public Path getToolPath() {
    return this.context.getSoftwarePath().resolve(this.tool);
  }

  /**
   * @return the {@link PluginManager} implementing the plugin logic of this {@link PipBasedIdeToolCommandlet}.
   */
  protected PluginManager getPluginManager() {

    return this.pluginManager;
  }

  @Override
  public ToolPlugins getPlugins() {

    return this.pluginManager.getPlugins();
  }

  @Override
  public ToolPluginDescriptor getPlugin(String key) {
    return this.pluginManager.getPlugin(key);
  }

  @Override
  public boolean isPluginUrlNeeded() {

    return false;
  }

  @Override
  public Path getPluginsConfigPath() {

    return this.context.getSettingsPath().resolve(this.tool).resolve(IdeContext.FOLDER_PLUGINS);
  }

  /**
   * @return the {@link Path} to the python environment the plugins are installed into. Unlike for an {@link IdeToolCommandlet} this is not a folder owned by
   *     this tool but the shared python environment (containing {@code site-packages}) that also holds th IDE itself. It must therefore never be delted -
   *     plugins are removed via {@link #uninstallPlugin(ToolPluginDescriptor)} instead.
   */
  @Override
  public Path getPluginsInstallationPath() {

    return getParentTool().getToolPath();
  }

  @Override
  protected void postInstall(ToolInstallRequest request) {

    super.postInstall(request);
    if (!request.isAlreadyInstalled() || this.forcePluginReinstall.isTrue()) {
      this.pluginManager.resetPlugins();
    }
    installPlugins(getPlugins().getPlugins(), request.getProcessContext());
  }

  @Override
  public void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {

    this.pluginManager.installPlugins(plugins, pc);
  }

  @Override
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {
    ProcessResult result = runPluginPackageManager(PackageManagerRequest.TYPE_INSTALL, plugin, pc);
    if (result.isSuccessful()) {
      IdeLogLevel.SUCCESS.log(LOG, "Successfully installed plugin: {}", plugin.name());
      step.success();
      return true;
    }
    result.log(IdeLogLevel.DEBUG, IdeLogLevel.ERROR);
    step.error("Failed to install plugin {} ({}): exit code war {}", plugin.name(), plugin.id(), result.getExitCode());
    return false;
  }

  @Override
  public void installPlugin(ToolPluginDescriptor plugin, final Step step) {

    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI);
    ToolInstallRequest request = new ToolInstallRequest(true);
    request.setProcessContext(pc);
    install(request);
    installPlugin(plugin, step, pc);
  }

  @Override
  public void uninstallPlugin(ToolPluginDescriptor plugin) {

    ProcessResult result = runPluginPackageManager(PackageManagerRequest.TYPE_UNINSTALL, plugin, null);
    if (result.isSuccessful()) {
      IdeLogLevel.SUCCESS.log(LOG, "Successfully uninstalled plugin {}", plugin.name());
    } else {
      result.log(IdeLogLevel.DEBUG, IdeLogLevel.ERROR);
      LOG.error("Could not uninstall plugin {} ({}): exit code was {}", plugin.name(), plugin.id(), result.getExitCode());
    }
  }

  @Override
  public void deleteAllPlugins() {

    for (ToolPluginDescriptor plugin : getPlugins().getPlugins()) {
      Path markerFile = retrievePluginMarkerFilePath(plugin);
      if ((markerFile != null) && Files.exists(markerFile)) {
        uninstallPlugin(plugin);
      }
    }
  }

  /**
   * @param type the {@link PackageManagerRequest#getType() type} of the request ({@link PackageManagerRequest#TYPE_INSTALL install} or
   *     {@link PackageManagerRequest#TYPE_UNINSTALL uninstall}).
   * @param plugin the {@link ToolPluginDescriptor} to install or uninstall. Its {@link ToolPluginDescriptor#id() ID} is used as name of the python
   *     package.
   * @param pc the {@link ProcessContext} to use or {@code null} to create a new one.
   * @return the {@link Processresult}
   */
  private ProcessResult runPluginPackageManager(String type, ToolPluginDescriptor plugin, ProcessContext pc) {

    PackageManagerRequest request = new PackageManagerRequest(type, plugin.id()).setProcessMode(ProcessMode.DEFAULT_CAPTURE).setProcessContext(pc);
    if (PackageManagerRequest.TYPE_INSTALL.equals(type)) {
      String version = plugin.version();
      if ((version != null) && !version.isBlank()) {
        request.setVersion(VersionIdentifier.of(version));
      }
    }
    return runPackageManager(request);
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
  public void handleInstallForInactivePlugin(ToolPluginDescriptor plugin) {
    LOG.debug("Omitting installation of inactive plugin {} ({}).", plugin.name(), plugin.id());
  }
}
