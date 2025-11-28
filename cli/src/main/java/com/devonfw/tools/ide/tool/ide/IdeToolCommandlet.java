package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
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
    super.run();
  }

  @Override
  public ProcessResult runTool(String... args) {

    return runTool(ProcessMode.BACKGROUND, null, args);
  }

  @Override
  public ToolInstallation install(ToolInstallRequest request) {

    configureWorkspace();
    return super.install(request);
  }

  /**
   * Configure (initialize or update) the workspace for this IDE using the templates from the settings.
   */
  protected void configureWorkspace() {

    FileAccess fileAccess = this.context.getFileAccess();
    Path workspaceFolder = this.context.getWorkspacePath();
    if (!fileAccess.isExpectedFolder(workspaceFolder)) {
      this.context.warning("Current workspace does not exist: {}", workspaceFolder);
      return; // should actually never happen...
    }
    Step step = this.context.newStep("Configuring workspace " + workspaceFolder.getFileName() + " for IDE " + this.tool);
    step.run(() -> doMergeWorkspaceStep(step, workspaceFolder));
  }

  private void doMergeWorkspaceStep(Step step, Path workspaceFolder) {

    int errors = 0;
    errors = mergeWorkspace(this.context.getUserHomeIde(), workspaceFolder, errors);
    errors = mergeWorkspace(this.context.getSettingsPath(), workspaceFolder, errors);
    errors = mergeWorkspace(this.context.getConfPath(), workspaceFolder, errors);
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

  private int mergeWorkspace(Path configFolder, Path workspaceFolder, int errors) {

    int result = errors;
    result = mergeWorkspaceSingle(configFolder.resolve(IdeContext.FOLDER_WORKSPACE), workspaceFolder, result);
    result = mergeWorkspaceSingle(configFolder.resolve(this.tool).resolve(IdeContext.FOLDER_WORKSPACE), workspaceFolder, result);
    return result;
  }

  private int mergeWorkspaceSingle(Path templatesFolder, Path workspaceFolder, int errors) {

    Path setupFolder = templatesFolder.resolve(IdeContext.FOLDER_SETUP);
    Path updateFolder = templatesFolder.resolve(IdeContext.FOLDER_UPDATE);
    if (!Files.isDirectory(setupFolder) && !Files.isDirectory(updateFolder)) {
      this.context.trace("Skipping empty or non-existing workspace template folder {}.", templatesFolder);
      return errors;
    }
    this.context.debug("Merging workspace templates from {}...", templatesFolder);
    return errors + this.context.getWorkspaceMerger().merge(setupFolder, updateFolder, this.context.getVariables(), workspaceFolder);
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
