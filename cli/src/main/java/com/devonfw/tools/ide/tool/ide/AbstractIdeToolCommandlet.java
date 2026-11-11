package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.plugin.AbstractPluginBasedCommandlet;
import com.devonfw.tools.ide.tool.vscode.Vscode;

/**
 * {@link AbstractToolCommandlet} for an IDE (integrated development environment) such as {@link Eclipse}, {@link Vscode}, or {@link Intellij}.
 */
public abstract class AbstractIdeToolCommandlet extends AbstractPluginBasedCommandlet implements IdeToolCommandlet {

  private static final String OPTIONS_ENV_SUFFIX = "_OPTIONS";

  private final IdeWorkspaceConfigurer workspaceConfigurer;

  /** The {@link ProjectFolderProperty} for the optional {@code --project} flag. */
  public final ProjectFolderProperty project;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public AbstractIdeToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
    assert (hasIde(tags));
    // the --project option is always declared so the CLI parser consumes it natively (issue #2821) and never forwards it to the IDE binary;
    // whether the folder is actually opened is decided in getOpenPath()/validateOpenPath() via isSupportProjectProperty()
    this.project = new ProjectFolderProperty();
    add(this.project);
    this.workspaceConfigurer = new IdeWorkspaceConfigurer(context, tool);
  }

  /**
   * @return {@code true} if the --project option is supported, {@code false} otherwise.
   */
  protected boolean isSupportProjectProperty() {
    return true;
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

    // the optional --project flag is parsed natively by the CLI parser (see ProjectFolderProperty) and is therefore no longer part of args
    addIdeOptions(args);
    return runTool(ProcessMode.BACKGROUND, null, args);
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

    validateOpenPath();
    if ((processMode != null) && processMode.isBackground() && !hasExplicitProject()) {
      configureWorkspace();
    }
    return super.runTool(pc, processMode, args);
  }

  /**
   * @return the {@link Path} that this IDE launch will open - the single source of truth for "which folder does this launch open?". Returns the
   *     {@link #project explicit {@code --project} folder} (relative paths resolved against the current working directory) if the flag was given, or the
   *     {@link IdeContext#getWorkspacePath() workspace path} otherwise. The IDE-specific properties environment variables (e.g. {@code IDEA_PROPERTIES})
   *     point at this same folder.
   */
  public Path getOpenPath() {

    Path resolved = null;
    if (isSupportProjectProperty()) {
      resolved = this.project.getValue();
    }
    if (resolved == null) {
      return this.context.getWorkspacePath();
    }
    if (!resolved.isAbsolute()) {
      Path cwd = this.context.getCwd();
      resolved = (cwd != null) ? cwd.resolve(resolved) : resolved;
    }
    return resolved.normalize();
  }

  /**
   * Validates the {@link #getOpenPath() folder to open} if an explicit {@code --project} flag was given. Aborts the launch (throwing a
   * {@link CliException}) when the folder does not exist or is not a directory, so the IDE is never started in such a case.
   */
  protected void validateOpenPath() {

    if (!isSupportProjectProperty() || this.project.getValue() == null) {
      return; // no explicit folder - use the default workspace as before
    }
    Path openPath = getOpenPath();
    if (!Files.exists(openPath)) {
      throw new CliException("Cannot open project " + openPath + " because this folder does not exist.");
    }
    if (!Files.isDirectory(openPath)) {
      throw new CliException("Cannot open project " + openPath + " because this path is not a folder.");
    }
  }

  @Override
  protected void postInstall(ToolInstallRequest request) {
    if (!hasExplicitProject()) {
      // an explicit --project launch opens an external folder that must not be written into, so skip the workspace configuration
      // (template merge and extra-SDK sync); a plain `ide install` still merges into the managed workspace
      configureWorkspace();
    }
    super.postInstall(request);
  }

  /**
   * @return {@code true} if an explicit {@code --project} folder was given for this run. Such a launch opens an external folder that IDEasy must not write
   *     into, so it skips the workspace configuration ({@link #configureWorkspace() template merge and extra-SDK sync}); a plain {@code ide install}
   *     (no flag) still merges into the managed workspace.
   */
  private boolean hasExplicitProject() {

    return this.project.getValue() != null;
  }

  /**
   * @return the {@link IdeWorkspaceConfigurer} for this IDE.
   */
  @Override
  public IdeWorkspaceConfigurer getWorkspaceConfigurer() {

    return this.workspaceConfigurer;
  }

  /**
   * Registers support for synchronizing an extra SDK/template for this IDE.
   *
   * <p>
   * The registered template path must be relative to the IDE workspace root. During workspace synchronization, the generic extra-SDK handling performed by the
   * {@link IdeWorkspaceConfigurer} uses this mapping to locate the corresponding template file in the settings repository and merge it into the current
   * workspace.
   * </p>
   *
   * @param sdk the name of the extra SDK/tool as configured in {@code ide-extra-tools.json}.
   * @param relativeTemplatePath the workspace-relative path of the IDE-specific template file to merge.
   */
  protected void registerExtraSdkTemplate(String sdk, Path relativeTemplatePath) {

    this.workspaceConfigurer.registerExtraSdkTemplate(sdk, relativeTemplatePath);
  }
}
