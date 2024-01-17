package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.vscode.Vscode;

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
  public IdeToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
    assert (tags.contains(Tag.IDE));
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
