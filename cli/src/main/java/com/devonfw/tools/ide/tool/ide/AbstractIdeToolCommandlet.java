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

  /** The bare {@code --project} flag, consumed by IDEasy so the IDE opens the current working directory instead of the default workspace. */
  private static final String PROJECT_OPTION = "--project";

  /** The {@code --project=} form of {@link #PROJECT_OPTION} that carries the folder as its value. */
  private static final String PROJECT_OPTION_PREFIX = "--project=";

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

    // mustExist=false on purpose: the value is set in extractProjectOption() after the framework validation, so the property's own
    // path checks never inspect it; validateOpenPath() is the real guard for the opened folder
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
   * Extracts the optional {@code --project} flag from the given launch arguments and remembers the folder it selects via {@link #project}, returning the
   * remaining arguments to pass through to the IDE. The flag is consumed by IDEasy (and never passed to the IDE) because the trailing arguments of an IDE
   * command are passed verbatim to the IDE binary. Two forms are supported: {@code --project} opens the current working directory, while
   * {@code --project=<path>} opens the given folder (absolute, a {@code ~/} or bare {@code ~} prefix resolved against the user home, or relative
   * resolved against the current working directory).
   *
   * @param args the raw command-line arguments to launch this IDE.
   * @return a copy of {@code args} with the {@code --project} flag removed (if present).
   */
  private List<String> extractProjectOption(List<String> args) {

    List<String> effectiveArgs = new ArrayList<>(args);
    for (int i = 0; i < effectiveArgs.size(); i++) {
      String arg = effectiveArgs.get(i);
      if (PROJECT_OPTION.equals(arg)) {
        this.project.setValue(this.context.getCwd());
        effectiveArgs.remove(i);
      } else if (arg.startsWith(PROJECT_OPTION_PREFIX)) {
        String folder = arg.substring(PROJECT_OPTION_PREFIX.length());
        if (folder.isEmpty()) {
          throw new CliException(
              "The " + PROJECT_OPTION_PREFIX + " option of command '" + this.tool + "' is missing the folder to open. Use '" + PROJECT_OPTION
                  + "' to open the current folder.");
        }
        // the shell does not expand '~' inside '--project=<value>', so resolve it against the user home here
        if ("~".equals(folder) || folder.startsWith("~/")) {
          folder = this.context.getUserHome() + folder.substring(1);
        }
        this.project.setValue(Path.of(folder));
        effectiveArgs.remove(i);
      }
    }
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
   * @return the {@link Path} that this IDE launch will open - the single source of truth for "which folder does this launch open?". Returns the
   *     {@link #project explicit {@code --project} folder} (relative paths resolved against the current working directory) if the flag was given, or the
   *     {@link IdeContext#getWorkspacePath() workspace path} otherwise. The IDE-specific properties environment variables (e.g. {@code IDEA_PROPERTIES})
   *     point at this same folder.
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
