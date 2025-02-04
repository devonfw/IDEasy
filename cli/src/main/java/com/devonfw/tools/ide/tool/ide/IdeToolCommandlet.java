package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.vscode.Vscode;

/**
 * {@link ToolCommandlet} for an IDE (integrated development environment) such as {@link Eclipse}, {@link Vscode}, or {@link Intellij}.
 */
public abstract class IdeToolCommandlet extends PluginBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public IdeToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
    assert (hasIde(tags));
  }

  private boolean hasIde(Set<Tag> tags) {

    for (Tag tag : tags) {
      if (tag.isAncestorOf(Tag.IDE) || (tag == Tag.IDE)) {
        return true;
      }
    }
    throw new IllegalStateException("Tags of IdeTool has to be connected with tag IDE: " + tags);
  }

  @Override
  public final void run() {

    configureWorkspace();
    super.run();
  }

  @Override
  public void runTool(String... args) {

    runTool(ProcessMode.BACKGROUND, null, args);
  }

  /**
   * Configure the workspace for this IDE using the templates from the settings.
   */
  protected void configureWorkspace() {

    Path settingsWorkspaceFolder = this.context.getSettingsPath().resolve(this.tool)
        .resolve(IdeContext.FOLDER_WORKSPACE);
    Path genericWorkspaceFolder = this.context.getSettingsPath().resolve(IdeContext.FOLDER_WORKSPACE);
    Path workspaceUpdateFolder = genericWorkspaceFolder.resolve(IdeContext.FOLDER_UPDATE);
    Path workspaceSetupFolder = genericWorkspaceFolder.resolve(IdeContext.FOLDER_SETUP);
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
    try (Step step = this.context.newStep("Configuring workspace " + ideWorkspacePath.getFileName() + " for IDE " + this.tool)) {
      int errors = this.context.getWorkspaceMerger().merge(workspaceSetupFolder, workspaceUpdateFolder, this.context.getVariables(), ideWorkspacePath);
      errors += this.context.getWorkspaceMerger().merge(setupFolder, updateFolder, this.context.getVariables(), ideWorkspacePath);
      if (errors == 0) {
        step.success();
      } else {
        step.error("Your workspace configuration failed with {} error(s) - see log above.\n"
            + "This is either a configuration error in your settings git repository or a bug in IDEasy.\n"
            + "Please analyze the above errors with your team or IDE-admin and try to fix the problem.", errors);
        this.context.askToContinue(
            "In order to prevent you from being blocked, you can start your IDE anyhow but some configuration may not be in sync.");
      }
    }
  }

  /**
   * Imports the repository specified by the given {@link Path} into the IDE managed by this {@link IdeToolCommandlet}.
   *
   * @param repositoryPath the {@link Path} to the repository directory to import.
   */
  public void importRepository(Path repositoryPath) {

    throw new UnsupportedOperationException("Repository import is not yet implemented for IDE " + this.tool);
  }
}
