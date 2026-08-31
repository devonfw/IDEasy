package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.vscode.Vscode;

/**
 * {@link ToolCommandlet} for an IDE (integrated development environment) such as {@link Eclipse}, {@link Vscode}, or {@link Intellij}.
 */
public abstract class IdeToolCommandlet extends PluginBasedCommandlet implements IdeFeatures {

  private static final String OPTIONS_ENV_SUFFIX = "_OPTIONS";

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

    List<String> effectiveArgs = new ArrayList<>(args);
    addIdeOptions(effectiveArgs);
    return runTool(ProcessMode.BACKGROUND, null, effectiveArgs);
  }

  /**
   * Appends the tokens of {@code «IDE»_OPTIONS} (e.g. {@code INTELLIJ_OPTIONS}) to the given {@code args}. This is the per-tool analogue of the global
   * {@code IDE_OPTIONS} and only applies when actually starting the IDE (not for internal calls like plugin installation or repository import).
   *
   * @param args the command-line arguments to launch this IDE, extended in place.
   */
  private void addIdeOptions(List<String> args) {

    String variableName = EnvironmentVariables.getToolVariablePrefix(this.tool) + OPTIONS_ENV_SUFFIX;
    String options = this.context.getVariables().get(variableName);
    if ((options != null) && !options.isBlank()) {
      for (String option : options.trim().split("\\s+")) {
        args.add(option);
      }
    }
  }

  @Override
  public ProcessResult runTool(ProcessContext pc, ProcessMode processMode, List<String> args) {

    if ((processMode != null) && processMode.isBackground()) {
      configureWorkspace();
    }
    return super.runTool(pc, processMode, args);
  }

  @Override
  protected void postInstall(ToolInstallRequest request) {
    configureWorkspace();
    super.postInstall(request);
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
   * Registers support for synchronizing an extra SDK/template for this IDE.
   *
   * <p>
   * The registered template path must be relative to the IDE workspace root. During workspace synchronization, the generic extra-SDK handling performed by
   * the {@link IdeWorkspaceConfigurer} uses this mapping to locate the corresponding template file in the settings repository and merge it into the current
   * workspace.
   * </p>
   *
   * @param sdk the name of the extra SDK/tool as configured in {@code ide-extra-tools.json}.
   * @param relativeTemplatePath the workspace-relative path of the IDE-specific template file to merge.
   */
  protected void registerExtraSdkTemplate(String sdk, Path relativeTemplatePath) {

    this.workspaceConfigurer.registerExtraSdkTemplate(sdk, relativeTemplatePath);
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
