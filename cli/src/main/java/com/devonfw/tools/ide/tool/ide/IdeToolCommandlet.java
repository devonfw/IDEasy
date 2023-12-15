package com.devonfw.tools.ide.tool.ide;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.vscode.Vscode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * {@link ToolCommandlet} for an IDE (integrated development environment) such as {@link Eclipse}, {@link Vscode}, or
 * {@link Intellij}.
 */
public abstract class IdeToolCommandlet extends PluginBasedCommandlet {

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

  @Override
  public void run() {

    configureWorkspace();
    runIde(this.arguments.asArray());
  }

  /**
   * Run the actual IDE.
   *
   * @param args the additional arguments to pass to the IDE.
   */
  protected void runIde(String... args) {

    runTool(null, args);
  }

  /**
   * Configure the workspace for this IDE using the templates from the settings.
   */
  protected void configureWorkspace() {

    Path settingsWorkspaceFolder = this.context.getSettingsPath().resolve(this.tool)
        .resolve(IdeContext.FOLDER_WORKSPACE);
    FileAccess fileAccess = this.context.getFileAccess();
    if (!fileAccess.isExpectedFolder(settingsWorkspaceFolder)) {
      return;
    }
    Path setupFolder = settingsWorkspaceFolder.resolve(IdeContext.FOLDER_SETUP);
    Path updateFolder = settingsWorkspaceFolder.resolve(IdeContext.FOLDER_UPDATE);
    if (!fileAccess.isExpectedFolder(setupFolder) && !fileAccess.isExpectedFolder(updateFolder)) {
      return;
    }
    Path ideWorkspacePath = this.context.getWorkspacePath();
    if (!fileAccess.isExpectedFolder(ideWorkspacePath)) {
      return; // should actually never happen...
    }
    this.context.step("Configuring workspace {} for IDE {}", ideWorkspacePath.getFileName(), this.tool);
    this.context.getWorkspaceMerger().merge(setupFolder, updateFolder, this.context.getVariables(), ideWorkspacePath);
  }

}
