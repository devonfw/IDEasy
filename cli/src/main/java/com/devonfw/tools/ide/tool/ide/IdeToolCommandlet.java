package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.CommandletManager;
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
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
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
   * <p>
   * The repository is searched for a build descriptor of any build tool that this IDE supports via {@link #getBuildTool2TemplateMap()}.
   * The first match triggers a merge of the corresponding template into the workspace via {@link #mergeTemplate(Path, String)}.
   * If no build tool of this IDE applies the repository is skipped.
   * </p>
   *
   * @param repositoryPath the {@link Path} to the repository directory to import.
   */
  public void importRepository(Path repositoryPath) {

    CommandletManager commandletManager = this.context.getCommandletManager();
    for (Entry<Class<? extends LocalToolCommandlet>, String> entry : getBuildTool2TemplateMap().entrySet()) {
      LocalToolCommandlet buildTool = commandletManager.getCommandlet(entry.getKey());
      Path buildDescriptor = buildTool.findBuildDescriptor(repositoryPath);
      if (buildDescriptor != null) {
        String templateFilename = entry.getValue();
        LOG.debug("Found build descriptor {} so merging template {}", buildDescriptor, templateFilename);
        mergeTemplate(repositoryPath, templateFilename);
        return;
      }
    }
    LOG.warn("No supported build descriptor was found for project import in {}", repositoryPath);
  }

  /**
   * @return the mapping of supported build tool commandlets to the template file name to be merged into the workspace (see
   *     {@link #mergeTemplate(Path, String)}) when the corresponding build descriptor is present in the imported repository.
   *     The default is an empty map meaning that no build tool is supported for repository import by this IDE.
   */
  protected Map<Class<? extends LocalToolCommandlet>, String> getBuildTool2TemplateMap() {

    return Map.of();
  }

  /**
   * Merges the template with the given file name into the workspace for the imported repository. This is the IDE-specific part of
   * {@link #importRepository(Path)} and is called after a supported build descriptor was found.
   *
   * <p>
   * The template location is built dynamically from the tool name (see {@link #getTemplateFolder()}) and the template file name, so no per-IDE template path
   * constant is needed. The environment variables are created via {@link #getTemplateEnvironmentVariables(Path)} with the relative project path as
   * {@code PROJECT_PATH}.
   * </p>
   *
   * @param repositoryPath the {@link Path} to the imported repository directory.
   * @param templateFilename the file name of the workspace-relative template to merge (as configured in {@link #getBuildTool2TemplateMap()}).
   */
  protected void mergeTemplate(Path repositoryPath, String templateFilename) {

    String templateFolder = getTemplateFolder();
    if (templateFolder == null) {
      throw new UnsupportedOperationException("Repository import is not yet implemented for IDE " + this.tool);
    }
    Path templateFile = this.context.getSettingsPath()
        .resolve(this.tool)
        .resolve(IdeContext.FOLDER_WORKSPACE)
        .resolve(IdeContext.FOLDER_REPOSITORY)
        .resolve(templateFolder)
        .resolve(templateFilename);
    if (!Files.exists(templateFile)) {
      throw new CliException("Cannot import project into workspace: template file not found at " + templateFile + "\n"
          + "Please do an upstream merge of your settings git repository.");
    }
    Path workspacesPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES);
    Path workspacePath = this.context.getFileAccess().findAncestor(repositoryPath, workspacesPath, 1);
    if (workspacePath == null) {
      throw new CliException("Cannot import project into workspace: could not find workspace from " + repositoryPath);
    }
    EnvironmentVariables environmentVariables = getTemplateEnvironmentVariables(workspacePath.relativize(repositoryPath));
    Path workspaceFile = workspacePath.resolve(templateFolder).resolve(templateFilename);
    doMergeTemplate(templateFile, workspaceFile, environmentVariables);
  }

  /**
   * Performs the actual merge of the resolved template file into the workspace file. This is the only IDE-specific part of
   * {@link #mergeTemplate(Path, String)} as the merge algorithm differs per IDE (e.g. {@code JSON} vs {@code XML}).
   *
   * @param templateFile the resolved {@link Path} to the template file in the settings repository.
   * @param workspaceFile the {@link Path} to the target file in the workspace to merge the template into.
   * @param environmentVariables the {@link EnvironmentVariables} to resolve variables (e.g. {@code PROJECT_PATH}) in the template.
   */
  protected void doMergeTemplate(Path templateFile, Path workspaceFile, EnvironmentVariables environmentVariables) {

    throw new UnsupportedOperationException("Repository import is not yet implemented for IDE " + this.tool);
  }

  /**
   * @return the name of the IDE configuration folder (e.g. {@code .vscode} or {@code .idea}) inside which the repository workspace templates
   *     are stored and merged, or {@code null} if this IDE does not support repository import. This folder is used both in the settings
   *     repository to locate the template and in the workspace to store the merged result.
   */
  protected String getTemplateFolder() {

    return null;
  }

  /**
   * Creates {@link EnvironmentVariables} for resolving the imported repository workspace template with the relative project path as {@code PROJECT_PATH}.
   *
   * @param projectPath the relative {@link Path} from the workspace root to the repository.
   * @return the resolved {@link EnvironmentVariables}.
   */
  protected EnvironmentVariables getTemplateEnvironmentVariables(Path projectPath) {

    ExtensibleEnvironmentVariables environmentVariables = new ExtensibleEnvironmentVariables(
        (AbstractEnvironmentVariables) this.context.getVariables().getParent(), this.context);
    environmentVariables.setValue("PROJECT_PATH", projectPath.toString().replace('\\', '/'));
    return environmentVariables.resolved();
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
