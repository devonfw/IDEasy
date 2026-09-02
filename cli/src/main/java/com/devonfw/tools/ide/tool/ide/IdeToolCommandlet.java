package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.ExtensibleEnvironmentVariables;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.merge.xml.XmlMergeDocument;
import com.devonfw.tools.ide.merge.xml.XmlMerger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.extra.ExtraToolInstallation;
import com.devonfw.tools.ide.tool.extra.ExtraTools;
import com.devonfw.tools.ide.tool.extra.ExtraToolsMapper;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.vscode.Vscode;

/**
 * {@link ToolCommandlet} for an IDE (integrated development environment) such as {@link Eclipse}, {@link Vscode}, or {@link Intellij}.
 */
public abstract class IdeToolCommandlet extends PluginBasedCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(IdeToolCommandlet.class);

  private static final String OPTIONS_ENV_SUFFIX = "_OPTIONS";
  private final Map<String, Set<Path>> extraSdkMap;

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
    this.extraSdkMap = new HashMap<>();
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
  protected Path getIdeMetadataPath() {

    return this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(getName()).resolve(this.context.getWorkspaceName());
  }

  /**
   * Configure (initialize or update) the workspace for this IDE using the templates from the settings.
   */
  public void configureWorkspace() {

    FileAccess fileAccess = this.context.getFileAccess();
    Path workspaceFolder = this.context.getWorkspacePath();
    if (!fileAccess.isExpectedFolder(workspaceFolder)) {
      LOG.warn("Current workspace does not exist: {}", workspaceFolder);
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

    synchronizeExtraToolInstallations();

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
      LOG.trace("Skipping empty or non-existing workspace template folder {}.", templatesFolder);
      return errors;
    }
    LOG.debug("Merging workspace templates from {}...", templatesFolder);
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

  /**
   * Registers support for synchronizing an extra SDK/template for this IDE.
   *
   * <p>
   * The registered template path must be relative to the IDE workspace root. During workspace synchronization, the generic extra-SDK handling in
   * {@link #synchronizeExtraToolInstallations()} uses this mapping to locate the corresponding template file in the settings repository and merge it into the
   * current workspace.
   * </p>
   *
   * @param sdk the name of the extra SDK/tool as configured in {@code ide-extra-tools.json}.
   * @param relativeTemplatePath the workspace-relative path of the IDE-specific template file to merge.
   */
  protected void registerExtraSdkTemplate(String sdk, Path relativeTemplatePath) {

    Set<Path> templatePaths = this.extraSdkMap.computeIfAbsent(sdk, _ -> new HashSet<>());
    templatePaths.add(relativeTemplatePath);
  }

  /**
   * Synchronizes extra IDEasy tool installations into the current IDE workspace configuration if supported.
   *
   * <p>
   * By default, nothing will happen. Your IDE commandlet has to register one or more according templates in its constructor.
   * </p>
   */
  protected void synchronizeExtraToolInstallations() {

    ExtraTools extraTools = ExtraToolsMapper.get().loadJsonFromFolder(this.context.getSettingsPath());
    if (extraTools == null) {
      return;
    }
    for (String sdk : extraTools.getSortedToolNames()) {
      Set<Path> templatePaths = this.extraSdkMap.get(sdk);
      if ((templatePaths == null) || templatePaths.isEmpty()) {
        LOG.debug("Skipping import of extra tool {} into {} because not configured or supported.", sdk, this.tool);
        continue;
      }
      List<ExtraToolInstallation> extraInstallations = extraTools.getExtraInstallations(sdk);
      synchronizeExtraToolInstallation(sdk, templatePaths, extraInstallations);
    }
  }

  private void synchronizeExtraToolInstallation(String sdk, Set<Path> templatePaths, List<ExtraToolInstallation> extraInstallations) {

    for (Path templatePath : templatePaths) {
      Path workspaceFile = this.context.getWorkspacePath().resolve(templatePath);
      Path templateFile = this.context.getSettingsPath().resolve(this.tool).resolve(IdeContext.FOLDER_WORKSPACE)
          .resolve(IdeContext.FOLDER_REPOSITORY)
          .resolve(templatePath);
      if (Files.exists(templateFile)) {
        for (ExtraToolInstallation extraInstallation : extraInstallations) {
          synchronizeExtraToolInstallation(sdk, templateFile, workspaceFile, extraInstallation);
        }
      } else {
        LOG.warn("You are missing a template file at {}.", templatePath);
        IdeLogLevel.INTERACTION.log(LOG, "Please ask the IDEasy admin in your project to merge your settings with upstream.");
      }
    }
  }

  private void synchronizeExtraToolInstallation(String sdk, Path templateFile, Path workspaceFile, ExtraToolInstallation installation) {

    String name = installation.name();
    Path extraToolHome = this.context.getSoftwareExtraPath().resolve(sdk).resolve(name);
    if (!Files.isDirectory(extraToolHome)) {
      LOG.warn("Skipping extra tool installation import to {} because it is missing at {}", this.tool, extraToolHome);
      IdeLogLevel.INTERACTION.log(LOG, "Please run the following command to fix:\nide update");
      return;
    }
    ExtensibleEnvironmentVariables environmentVariables = new ExtensibleEnvironmentVariables(
        (AbstractEnvironmentVariables) this.context.getVariables().getParent(), this.context);
    String variablePrefix = "EXTRA_" + sdk.toUpperCase(Locale.ROOT);
    environmentVariables.setValue(variablePrefix + "_NAME", name);
    environmentVariables.setValue(variablePrefix + "_HOME", extraToolHome.toString().replace('\\', '/'));
    environmentVariables.setValue(variablePrefix + "_VERSION", installation.version().toString());
    if (installation.edition() != null) {
      environmentVariables.setValue(variablePrefix + "_EDITION", installation.edition());
    }

    XmlMerger xmlMerger = new XmlMerger(this.context);
    XmlMergeDocument workspaceDocument = xmlMerger.load(workspaceFile);
    XmlMergeDocument templateDocument = xmlMerger.loadAndResolve(templateFile, environmentVariables);
    Document mergedDocument = xmlMerger.merge(templateDocument, workspaceDocument, false);
    xmlMerger.save(mergedDocument, workspaceFile);
  }
}
