package com.devonfw.tools.ide.tool.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
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
import com.devonfw.tools.ide.version.VersionSegment;

/**
 * Base class for {@link AbstractLocalToolCommandlet}s that support plugins. It can automatically install configured plugins for the tool managed by this
 * commandlet.
 */
public abstract class AbstractPluginBasedCommandlet extends AbstractLocalToolCommandlet implements PluginBasedCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractPluginBasedCommandlet.class);

  private final PluginManager pluginManager;

  /** {@link FlagProperty} to force the reset and reinstallation of plugins as configured in the project settings. */
  public final FlagProperty forcePluginReinstall;

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
    this.forcePluginReinstall = add(new FlagProperty("--force-plugin-reinstall"));
  }

  /**
   * @return the {@link PluginManager} for this tool.
   */
  @Override
  public PluginManager getPluginManager() {

    return this.pluginManager;
  }

  /**
   * @return {@code true} if the reset and reinstallation of the plugins is forced for the current run via the {@code --force-plugin-reinstall} flag.
   */
  @Override
  public boolean isForcePluginReinstall() {

    return this.forcePluginReinstall.isTrue();
  }

  @Override
  protected void postInstall(ToolInstallRequest request) {

    super.postInstall(request);
    Path pluginsPath = getPluginsInstallationPath();
    if (isPluginPurgeRequired(request)) {
      this.pluginManager.resetPlugins(request);
    } else if (!Files.isDirectory(pluginsPath)) {
      // marker files are stale if their plugins folder is missing (e.g. plugins installed by an older IDEasy release directly into plugins/«tool»)
      this.pluginManager.deleteAllPluginMarkerFiles();
    }
    this.context.getFileAccess().mkdirs(pluginsPath);
    installPlugins(getPlugins().getPlugins(), request.getProcessContext());
    deleteObsoletePlugins(pluginsPath);
  }

  /**
   * Deletes the plugins of other (previously installed) versions of this tool. As these may still be in use by a running instance of the IDE that keeps files
   * locked (on Windows), this is only done on a best-effort basis and will be retried on the next installation or update.
   *
   * @param pluginsPath the {@link #getPluginsInstallationPath() plugins installation path} of the current version to keep.
   */
  private void deleteObsoletePlugins(Path pluginsPath) {

    Path toolPluginsPath = this.context.getPluginsPath().resolve(this.tool);
    if (pluginsPath.equals(toolPluginsPath)) {
      return; // version unknown, so we cannot tell which plugins are obsolete
    }
    FileAccess fileAccess = this.context.getFileAccess();
    for (Path obsoletePlugins : fileAccess.listChildren(toolPluginsPath, path -> !path.equals(pluginsPath))) {
      try {
        fileAccess.delete(obsoletePlugins);
      } catch (RuntimeException e) {
        LOG.warn("Could not delete obsolete plugins at {} as they may still be in use. They will be cleaned up on a later update.", obsoletePlugins, e);
      }
    }
  }

  /**
   * @return the {@link Path} where the plugins of the installed version are located. Plugins are installed per version up to the
   *     {@link #getIncompatibleVersionSegmentIndex() incompatible segment} (e.g. {@code plugins/intellij/2026.1} for IntelliJ 2026.1.4). This way, an update to
   *     a compatible version keeps the plugins, while an update to an incompatible version installs the plugins into a new folder so that the folder of the
   *     previous version does not have to be deleted while it may still be in use by a running IDE.
   */
  @Override
  public Path getPluginsInstallationPath() {

    Path toolPluginsPath = this.context.getPluginsPath().resolve(this.tool);
    VersionIdentifier version = getInstalledVersion();
    if (version == null) {
      return toolPluginsPath;
    }
    StringBuilder pluginsVersion = new StringBuilder();
    VersionSegment segment = version.getStart();
    for (int i = 0; (segment != null) && (i <= getIncompatibleVersionSegmentIndex()); i++) {
      pluginsVersion.append(segment);
      segment = segment.getNextOrNull();
    }
    return toolPluginsPath.resolve(pluginsVersion.toString());
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

    Path pluginsPath = getPluginsInstallationPath();
    try {
      this.context.getFileAccess().delete(pluginsPath);
    } catch (RuntimeException e) {
      // the plugins may still be in use by a running instance of the IDE that keeps the files locked (on Windows); they will be removed on a later
      // installation or update
      LOG.warn("Could not delete all plugins at {} as they may still be in use. They will be cleaned up on a later update.", pluginsPath, e);
    }
  }
}
