package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Base class for {@link AbstractLocalToolCommandlet}s that support plugins. It can automatically install configured plugins for the tool managed by this
 * commandlet.
 */
public abstract class AbstractPluginBasedCommandlet extends AbstractLocalToolCommandlet implements PluginBasedCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractPluginBasedCommandlet.class);

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
  public AbstractPluginBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

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
  @Override
  public PluginManager getPluginManager() {

    return this.pluginManager;
  }

  @Override
  protected void postInstall(ToolInstallRequest request) {

    super.postInstall(request);

    VersionIdentifier version = null;
    if (request.getRequested() != null) {
      version = request.getRequested().getResolvedVersion();
    }
    if (version == null) {
      version = getInstalledVersion();
    }

    Path pluginsInstallationPath = getPluginsInstallationPath(version);

    if (!request.isAlreadyInstalled() || this.forcePluginReinstall.isTrue()) {
      LOG.info("Resetting installed plugins for the current IDE version...");
      this.context.getFileAccess().delete(pluginsInstallationPath);
      this.pluginManager.deletePluginMarkerFiles(version == null ? null : version.toString());
    }

    this.context.getFileAccess().mkdirs(pluginsInstallationPath);
    installPlugins(getPlugins().getPlugins(), request.getProcessContext());

    if (version != null) {
      cleanupOldPluginVersions(version);
    }
  }

  /**
   * @param plugins the configured {@link ToolPluginDescriptor plugins} used to detect undefined entries.
   * @return the {@link Set} of {@link ToolPluginDescriptor#name() plugin names} configured in the tool-specific {@code «TOOL»_EXTRA_PLUGINS} variable (e.g.
   *     {@code VSCODE_EXTRA_PLUGINS=copilot,docker}). This allows a user to permanently opt-in to plugins that are not
   *     {@link ToolPluginDescriptor#active() active} in the project settings, without modifying the shared settings and without losing them when plugins are
   *     purged and reinstalled on IDE upgrade. Values refer to the {@link ToolPluginDescriptor#name() name} of the plugin (the filename of its
   *     {@code .properties} file) and not to the {@link ToolPluginDescriptor#id() id}. Names that do not resolve to a configured plugin are logged as a warning
   *     and skipped so that a single stale entry cannot break the entire installation.
   */
  protected Set<String> getExtraPlugins(Collection<ToolPluginDescriptor> plugins) {

    return this.pluginManager.getExtraPlugins(plugins);
  }

  @Override
  public Path getPluginsInstallationPath() {

    VersionIdentifier version = getInstalledVersion();
    return getPluginsInstallationPath(version);
  }

  /**
   * @param version the tool version.
   * @return the plugin installation path for the given tool version.
   */
  public Path getPluginsInstallationPath(VersionIdentifier version) {

    Path pluginsPath = this.context.getPluginsPath().resolve(this.tool);
    if (version == null) {
      return pluginsPath;
    }
    return pluginsPath.resolve(version.toString());
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

    this.pluginManager.uninstallPlugin(plugin);
  }

  @Override
  public void deleteAllPlugins() {

    this.context.getFileAccess().delete(getPluginsInstallationPath());
  }

  @Override
  public VersionIdentifier getPluginMarkerVersion() {

    return getInstalledVersion();
  }

  private void cleanupOldPluginVersions(VersionIdentifier currentVersion) {

    Path toolPluginsPath = this.context.getPluginsPath().resolve(this.tool);
    FileAccess fileAccess = this.context.getFileAccess();

    if (!Files.isDirectory(toolPluginsPath)) {
      return;
    }

    List<Path> versionDirectories =
        fileAccess.listChildren(toolPluginsPath, Files::isDirectory);

    for (Path versionDirectory : versionDirectories) {
      String version = versionDirectory.getFileName().toString();

      if (version.equals(currentVersion.toString())) {
        continue;
      }

      try {
        fileAccess.delete(versionDirectory);
        this.pluginManager.deletePluginMarkerFiles(version);
        LOG.debug("Deleted obsolete plugin directory {}.", versionDirectory);
      } catch (RuntimeException _) {
        LOG.warn(
            "Could not delete obsolete plugin directory {}. It may still be in use and will be cleaned up later.",
            versionDirectory);
      }
    }
  }
}
