package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.FolderProperty;
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

  /** The {@code --project} flag consumed by IDEasy to select the folder the IDE opens instead of the default workspace. */
  private static final String PROJECT_OPTION = "--project";

  private final IdeWorkspaceConfigurer workspaceConfigurer;

  /** The {@link FolderProperty} for the optional {@code --project} flag. */
  protected FolderProperty project;

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
    this.workspaceConfigurer = new IdeWorkspaceConfigurer(context, tool);
  }

  @Override
  protected void initProperties() {

    // mustExist=false on purpose: the value is only set in extractProjectOption() (after CLI validation), so the property's own
    // exists/is-directory check would never fire anyway; validateOpenPath() is the real guard for the opened folder
    this.project = add(new FolderProperty("--project", false, null, false));
    super.initProperties();
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

    List<String> effectiveArgs = extractProjectOption(args);
    addIdeOptions(effectiveArgs);
    return runTool(ProcessMode.BACKGROUND, null, effectiveArgs);
  }

  /**
   * Extracts the optional {@code --project} flag from the given launch arguments and remembers its folder value via {@link #project}, returning the
   * remaining arguments that should be passed through to the IDE. Because the remaining trailing arguments of an IDE command are passed verbatim to the IDE
   * binary, this flag is consumed by IDEasy and never passed through to the IDE.
   *
   * @param args the raw command-line arguments to launch this IDE.
   * @return a copy of {@code args} with the {@code --project} flag and its value removed (if present).
   */
  private List<String> extractProjectOption(List<String> args) {

    List<String> effectiveArgs = new ArrayList<>(args);
    int optionIndex = effectiveArgs.indexOf(PROJECT_OPTION);
    if (optionIndex < 0) {
      return effectiveArgs;
    }
    int valueIndex = optionIndex + 1;
    if (valueIndex >= effectiveArgs.size()) {
      throw new CliException("Missing folder argument for the " + PROJECT_OPTION + " option of command '" + this.tool + "'.");
    }
    this.project.setValue(Path.of(effectiveArgs.get(valueIndex)));
    effectiveArgs.remove(valueIndex); // remove the value first so the option index stays valid
    effectiveArgs.remove(optionIndex);
    return effectiveArgs;
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
   * @return the {@link Path} that this IDE launch will open. This is the single source of truth for "which folder does this launch open?": it returns the
   *     {@link #project explicit {@code --project} folder} (relative paths resolved against the current working directory) if the flag was given, or the
   *     {@link IdeContext#getWorkspacePath() workspace path} otherwise (today's default behavior). The IDE-specific property environment variables
   *     (e.g. {@code IDEA_PROPERTIES}) point at this same folder so the shell env and the launch agree: the opened project's own properties file when a
   *     {@code --project} folder was given, the managed workspace's otherwise.
   */
  public Path getOpenPath() {

    Path resolved = this.project.getValue();
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

    if (this.project.getValue() == null) {
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
      // (template merge and extra-SDK sync); a plain `ide install` keeps merging into the managed workspace as today
      configureWorkspace();
    }
    super.postInstall(request);
  }

  /**
   * @return {@code true} if an explicit {@code --project} folder was given for this run, i.e. the launch opens an external folder that IDEasy must not
   *     write into. Such launches skip the workspace configuration ({@link #configureWorkspace() template merge and extra-SDK sync}) on every call site of
   *     the launch path (the {@link #runTool} seam and the {@link #postInstall postInstall} of the launch's ensure-install step); a plain
   *     {@code ide install} (no flag) still merges into the managed workspace.
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
