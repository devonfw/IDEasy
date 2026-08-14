package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
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
public abstract class IdeToolCommandlet extends PluginBasedCommandlet implements IdeFeatures {

  private final IdeWorkspaceConfigurer workspaceConfigurer;

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
    this.workspaceConfigurer = new IdeWorkspaceConfigurer(context, tool);
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
  protected final void doRun() {
    super.doRun();
  }

  @Override
  public ProcessResult runTool(List<String> args) {

    return runTool(ProcessMode.BACKGROUND, null, args);
  }

  @Override
  public ToolInstallation install(ToolInstallRequest request) {

    configureWorkspace();
    return super.install(request);
  }

  /**
   * @return the {@link Path} to the IDE-specific metadata folder for the {@link IdeContext#getWorkspaceName() current workspace}, located at
   *     {@code $IDE_HOME/.ide/«ide»/«workspace»}. Unlike {@link IdeContext#getWorkspacePath() the workspace path} (which holds the projects to open), this
   *     folder keeps IDE-specific metadata (e.g. {@code .vmoptions} or {@code *.properties} files) out of the workspace so it stays clean and independent of
   *     the IDE being used.
   */
  @Override
  public Path getIdeMetadataPath() {

    return this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(getName()).resolve(this.context.getWorkspaceName());
  }

  /**
   * Configure (initialize or update) the workspace for this IDE using the templates from the settings.
   */
  @Override
  public void configureWorkspace() {
    this.workspaceConfigurer.configureWorkspace();
  }


  /**
   * Imports the repository specified by the given {@link Path} into the IDE managed by this {@link IdeToolCommandlet}.
   *
   * @param repositoryPath the {@link Path} to the repository directory to import.
   */
  @Override
  public void importRepository(Path repositoryPath) {

    throw new UnsupportedOperationException("Repository import is not yet implemented for IDE " + this.tool);
  }
}
