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
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} for tools that support plugin management.
 * <p>
 * This follows the same pattern as {@link com.devonfw.tools.ide.tool.ide.IdeToolCommandlet}, decoupling plugin capabilities from the installation mechanism.
 * Both binary-installed IDEs (VS Code, IntelliJ) and package-manager-installed tools (Spyder via pip) can support plugins by composing a
 * {@link PluginManager}.
 * </p>
 */
public interface PluginBasedCommandlet extends LocalToolCommandlet {

  /**
   * @return the {@link PluginManager} implementing the plugin logic of the tool. Needed so that the default methods of this interface can delegate the shared
   *     plugin behaviour to a single implementation.
   */
  PluginManager getPluginManager();

  /**
   * @return the configured edition of the tool owning the plugins (see {@code AbstractToolCommandlet#getConfiguredEdition()}).
   */
  String getConfiguredEdition();

  /**
   * @return the installed edition of the tool owning the plugins or {@code null} if not installed (see {@code AbstractToolCommandlet#getInstalledEdition()}).
   */
  String getInstalledEdition();

  /**
   * @return the {@link Path} to the folder with the plugin configuration files inside the settings. The default implementation is shared by all plugin-capable
   *     tools (see {@link AbstractPluginBasedCommandlet} and {@link com.devonfw.tools.ide.tool.pip.PipBasedIdeToolCommandlet}).
   */
  default Path getPluginsConfigPath() {

    return getContext().getSettingsPath().resolve(getName()).resolve(IdeContext.FOLDER_PLUGINS);
  }

  /**
   * @return the {@link Path} where the plugins of this tool shall be installed.
   */
  Path getPluginsInstallationPath();

  /**
   * @return {@code true} if the {@link ToolPluginDescriptor#url() plugin url} is needed, {@code false} otherwise. The default (no URL) is shared by all
   *     plugin-capable tools; IDEs that need a URL (e.g. Eclipse) override this.
   */
  default boolean isPluginUrlNeeded() {

    return false;
  }

  /**
   * @return the {@link ToolPlugins} configured for this tool. The default implementation is shared by all plugin-capable tools.
   */
  default ToolPlugins getPlugins() {
  protected Path getPluginsConfigPath() {

    return this.context.getSettingsPath().resolve(this.tool).resolve(IdeContext.FOLDER_PLUGINS);
  }

  private Path getUserHomePluginsConfigPath() {

    return this.context.getUserHomeIde().resolve(IdeContext.FOLDER_SETTINGS).resolve(this.tool).resolve(IdeContext.FOLDER_PLUGINS);
  }

  /**
   * @return the {@link Path} where the plugins of this {@link IdeToolCommandlet} shall be installed.
   */
  public Path getPluginsInstallationPath() {

    VersionIdentifier version = getInstalledVersion();
    if (version == null) {
      return this.context.getPluginsPath().resolve(this.tool);
    }
    return getPluginsInstallationPath(version);
  }

  public Path getPluginsInstallationPath(VersionIdentifier version) {

    if (version == null) {
      return this.context.getPluginsPath().resolve(this.tool);
    }

    return getPluginManager().getPlugins();
    return this.context.getPluginsPath()
        .resolve(this.tool)
        .resolve(version.toString());
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
      deleteAllPlugins(pluginsInstallationPath, version);
    }

    this.context.getFileAccess().mkdirs(pluginsInstallationPath);
    installPlugins(request.getProcessContext());

    if (version != null) {
      cleanupOldPluginVersions(version);
    }
  }

  /**
   * @param key the filename of the properties file configuring the requested plugin (typically excluding the ".properties" extension).
   * @return the {@link ToolPluginDescriptor} for the given {@code key}. The default implementation is shared by all plugin-capable tools.
   */
  default ToolPluginDescriptor getPlugin(String key) {
  private void deleteAllPlugins(Path pluginsInstallationPath, VersionIdentifier version) {

    return getPluginManager().getPlugin(key);
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.delete(pluginsInstallationPath);

    deletePluginMarkerFiles(version == null ? null : version.toString());
  }

  private void installPlugins(ProcessContext pc) {
    installPlugins(getPlugins().getPlugins(), pc);
  }

  /**
   * Installs the given active plugins and handles the inactive ones. The default implementation is shared by all plugin-capable tools.
   *
   * @param plugins the {@link Collection} of {@link ToolPluginDescriptor plugins} to install.
   * @param pc the {@link ProcessContext} to use.
   */
  default void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {

    getPluginManager().installPlugins(plugins, pc);
  }

  /**
   * Performs the tool-specific installation of a single plugin.
   * @param plugin the {@link ToolPluginDescriptor plugin} to search for.
   * @return Path to the plugin marker file.
   */
  public Path retrievePluginMarkerFilePath(ToolPluginDescriptor plugin) {
    if (this.context.getIdeHome() != null) {
      String markerFileName = "plugin"
          + "." + getName()
          + "." + getInstalledEdition();

      String ideVersion = getPluginMarkerVersionSegment();
      if (ideVersion != null) {
        markerFileName = markerFileName + "." + ideVersion;
      }

      markerFileName = markerFileName + "." + plugin.name();

      String version = plugin.version();
      if ((version != null) && !version.isBlank()) {
        markerFileName = markerFileName + ".version-" + normalizeMarkerFileSegment(version);
      }
      return this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(markerFileName);
    }
    return null;
  }

  private String normalizeMarkerFileSegment(String value) {
    // replace all characters that are not allowed in filenames with "_"
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

    String markerFilePrefix = "plugin"
        + "." + getName()
        + "." + getInstalledEdition();

    String ideVersion = getPluginMarkerVersionSegment();
    if (ideVersion != null) {
      markerFilePrefix = markerFilePrefix + "." + ideVersion;
    }

    markerFilePrefix = markerFilePrefix + "." + plugin.name();
    String finalMarkerFilePrefix = markerFilePrefix;

    List<Path> markerFiles = fileAccess.listChildren(currentMarkerFilePath.getParent(),
        p -> {
          String fileName = p.getFileName().toString();
          return Files.isRegularFile(p) && (fileName.equals(finalMarkerFilePrefix) || fileName.startsWith(finalMarkerFilePrefix + ".version-"));
        });

    for (Path markerFile : markerFiles) {
      if (!markerFile.equals(currentMarkerFilePath)) {
        fileAccess.delete(markerFile);
        LOG.debug("Deleted stale plugin marker file {} before creating {}.", markerFile, currentMarkerFilePath);
      }
    }
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   * @param pc the {@link ProcessContext} to use.
   * @return {@code true} if the installation of the plugin succeeded, {@code false} if not.
   */
  boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc);

  /**
   * Ensures that the tool itself is installed and then installs the plugin.
   *
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   */
  void installPlugin(ToolPluginDescriptor plugin, final Step step);

  /**
   * @param plugin the {@link ToolPluginDescriptor} to uninstall.
   */
  void uninstallPlugin(ToolPluginDescriptor plugin);

  /**
   * Uninstalls all currently installed plugins so that they can be installed again as configured in the project settings.
   */
  void deleteAllPlugins();

  /**
   * @param plugin the {@link ToolPluginDescriptor plugin} to search for.
   * @return the {@link Path} to the plugin marker file or {@code null} if we are not inside an IDEasy project. The default implementation is shared by all
   *     plugin-capable tools.
   */
  default Path retrievePluginMarkerFilePath(ToolPluginDescriptor plugin) {

    return getPluginManager().retrievePluginMarkerFilePath(plugin);
  }

  /**
   * Creates a marker file for a plugin in {@code $IDE_HOME/.ide/plugin.<<tool>>.<<edition>>.<<plugin-name>>}. The default implementation is shared by all
   * plugin-capable tools.
   *
   * @param plugin the plugin the {@link ToolPluginDescriptor plugin} for which the marker file should be created.
   */
  default void createPluginMarkerFile(ToolPluginDescriptor plugin) {

    getPluginManager().createPluginMarkerFile(plugin);
  }

  private void cleanupOldPluginVersions(VersionIdentifier currentVersion) {

    Path toolPluginsPath = this.context.getPluginsPath().resolve(this.tool);
    FileAccess fileAccess = this.context.getFileAccess();

    if (!Files.isDirectory(toolPluginsPath)) {
      return;
    }

    List<Path> versionDirectories = fileAccess.listChildren(toolPluginsPath, Files::isDirectory);

    for (Path versionDirectory : versionDirectories) {
      String version = versionDirectory.getFileName().toString();

      if (version.equals(currentVersion.toString())) {
        continue;
      }

      try {
        fileAccess.delete(versionDirectory);
        deletePluginMarkerFiles(version);
        LOG.debug("Deleted obsolete plugin directory {}.", versionDirectory);
      } catch (RuntimeException _) {
        LOG.warn("Could not delete obsolete plugin directory {}. It may still be in use and will be cleaned up later.",
            versionDirectory);
      }
    }
  }

  private String getPluginMarkerVersionSegment() {

    VersionIdentifier ideVersion = getInstalledVersion();
    if (ideVersion == null) {
      return null;
    }
    return normalizeMarkerFileSegment(ideVersion.toString());
  }

  /**
   * Deletes plugin marker files belonging to the specified IDE version.
   *
   * @param version the IDE version whose plugin marker files shall be deleted, or {@code null} to delete all plugin marker files for this tool.
   */
  private void deletePluginMarkerFiles(String version) {

    FileAccess fileAccess = this.context.getFileAccess();

    List<Path> markerFiles = fileAccess.listChildren(
        this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE),
        Files::isRegularFile);

    String markerPrefix = "plugin." + getName() + ".";
    String versionSegment = null;

    if (version != null) {
      versionSegment = "." + normalizeMarkerFileSegment(version) + ".";
    }

    for (Path markerFile : markerFiles) {
      String fileName = markerFile.getFileName().toString();

      boolean matches = fileName.startsWith(markerPrefix);

      if (versionSegment != null) {
        matches = matches && fileName.contains(versionSegment);
      }

      if (matches) {
        fileAccess.delete(markerFile);
        LOG.debug("Plugin marker file {} got deleted.", markerFile);
      }
    }
  }
}
