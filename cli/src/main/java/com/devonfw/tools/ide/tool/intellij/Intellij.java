package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
import com.devonfw.tools.ide.merge.xml.XmlMergeDocument;
import com.devonfw.tools.ide.merge.xml.XmlMerger;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.extra.ExtraToolInstallation;
import com.devonfw.tools.ide.tool.extra.ExtraTools;
import com.devonfw.tools.ide.tool.extra.ExtraToolsMapper;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeaBasedIdeToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Intellij.class);

  private static final VersionIdentifier INTELLIJ_LAST_SEPARATE_VERSION = VersionIdentifier.of("2025.2.6.1");

  private static final String IDEA = "idea";

  private static final String IDEA64_EXE = IDEA + "64.exe";

  private static final String IDEA_BASH_SCRIPT = IDEA + ".sh";

  private static final String FOLDER_IDEA_CONFIG = ".idea";
  private static final String TEMPLATE_LOCATION = "intellij/workspace/repository/" + FOLDER_IDEA_CONFIG;
  private static final String GRADLE_XML = "gradle.xml";
  private static final String MISC_XML = "misc.xml";
  private static final String IDEA_PROPERTIES = "idea.properties";

  private static final Map<Class<? extends LocalToolCommandlet>, String> BUILD_TOOL_TO_IJ_TEMPLATE = Map.of(Mvn.class, MISC_XML, Gradle.class, GRADLE_XML);

  /**
   * IntelliJ-specific configuration describing how an extra IDEasy tool installation can be imported as an SDK/tool definition into IntelliJ configuration.
   *
   * @param templateFile the IntelliJ XML template file used as merge source.
   * @param targetFile the IntelliJ configuration file relative to the workspace root that should be updated.
   * @param reservedName an optional reserved logical name that must not be reused by an extra installation because it would collide with the main SDK/tool
   *     definition.
   */
  private record IntellijExtraSdkConfig(String templateFile, String targetFile, String reservedName) {

  }

  /**
   * Mapping of IDEasy tool names to IntelliJ-specific SDK import configuration.
   *
   * <p>
   * Only tools contained in this map are imported automatically into IntelliJ. This keeps the import logic generic for IntelliJ while allowing support to be
   * added incrementally tool by tool.
   * </p>
   */
  private static final Map<String, IntellijExtraSdkConfig> EXTRA_TOOL_CONFIGS = Map.of(
      "java", new IntellijExtraSdkConfig("jdk-extra-java.xml", ".intellij/config/options/jdk.table.xml", "java")
  );

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Intellij(IdeContext context) {

    super(context, "intellij", Set.of(Tag.INTELLIJ));
  }

  @Override
  protected String getBinaryName() {

    if (this.context.getSystemInfo().isWindows()) {
      return IDEA64_EXE;
    } else {
      if (Files.exists(this.getToolBinPath().resolve(IDEA))) {
        return IDEA;
      } else if (Files.exists(this.getToolBinPath().resolve(IDEA_BASH_SCRIPT))) {
        return IDEA_BASH_SCRIPT;
      } else {
        return IDEA;
      }
    }
  }

  @Override
  protected String getIdeProductPrefix() {

    return IDEA;
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {
    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);

    Path workspacePath = this.context.getWorkspacePath();
    // Synchronize extra SDK/tool definitions before IntelliJ starts so newly installed extra tool versions
    // become visible in the IDE without requiring a separate repository import step.
    importExtraToolInstallationsToWorkspace(workspacePath);

    environmentContext.withEnvVar("IDEA_PROPERTIES", workspacePath.resolve(IDEA_PROPERTIES).toString());
  }

  @Override
  protected ToolEditionAndVersion adjustRequestedEdition(ToolEditionAndVersion requested) {

    ToolEdition edition = requested.getEdition();
    // Check if edition is set as "ultimate"
    if ("ultimate".equals(edition.edition())) {

      VersionIdentifier version;
      if (requested.getVersion() != null) {
        version = VersionIdentifier.of(requested.getVersion().toString());
      } else {
        version = getConfiguredVersion();
      }
      // Check whether set version warrants switching editions
      if ((version.isGreater(INTELLIJ_LAST_SEPARATE_VERSION)) ||
          // Specified version is > 2025.2.6.1 **OR** no specified version but configured version is > 2025.2.6.1
          (VersionIdentifier.LATEST.equals(version)) || // No version specified and no configured version
          (VersionIdentifier.LATEST_UNSTABLE.equals(version))) { // No version specified and no configured version
        // Switching to IntelliJ Standard edition
        LOG.warn("""
            Notice: You have configured IDEasy to use the IntelliJ Ultimate Edition. Since version 2025.3, the Ultimate and Community editions of IntelliJ have been unified into a single edition.
            Since you are attempting to install a version of IntelliJ that is 2025.3 or newer, we are automatically switching your edition to the unified edition to ensure compatibility.
            To specifically install the last true ultimate version of IntelliJ, please run "ide install intellij 2025.2.6.1".
            Otherwise, we recommend permanently switching to the unified edition by running "ide set-edition intellij intellij".""");
        edition = new ToolEdition(this.tool, "intellij");
        requested.replaceEdition(edition);
      }
    }
    return requested;
  }

  private EnvironmentVariables getIntellijEnvironmentVariables(Path projectPath) {
    ExtensibleEnvironmentVariables environmentVariables = new ExtensibleEnvironmentVariables(
        (AbstractEnvironmentVariables) this.context.getVariables().getParent(), this.context);

    environmentVariables.setValue("PROJECT_PATH", projectPath.toString().replace('\\', '/'));
    return environmentVariables.resolved();
  }

  private Path getWorkspacePath(Path repositoryPath) {
    Path workspacesPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES);
    Path workspacePath = this.context.getFileAccess().findAncestor(repositoryPath, workspacesPath, 1);
    if (workspacePath == null) {
      throw new CliException(
          "Cannot import project into workspace: could not find workspace from " + repositoryPath);
    }
    return workspacePath;
  }

  private void mergeConfig(Path repositoryPath, String configFilePath) {
    Path templatePath = this.context.getSettingsPath().resolve(TEMPLATE_LOCATION);
    Path templateFile = templatePath.resolve(configFilePath);
    if (!Files.exists(templateFile)) {
      throw new CliException(
          "Cannot import project into workspace: template file not found at " + templateFile + "\n"
              + "Please do an upstream merge of your settings git repository.");
    }
    Path workspacePath = getWorkspacePath(repositoryPath);
    XmlMerger xmlMerger = new XmlMerger(this.context);
    EnvironmentVariables environmentVariables = getIntellijEnvironmentVariables(workspacePath.relativize(repositoryPath));
    Path workspaceFile = workspacePath.resolve(FOLDER_IDEA_CONFIG).resolve(configFilePath);

    XmlMergeDocument workspaceDocument = xmlMerger.load(workspaceFile);
    XmlMergeDocument templateDocument = xmlMerger.loadAndResolve(templateFile, environmentVariables);

    Document mergedDocument = xmlMerger.merge(templateDocument, workspaceDocument, false);

    xmlMerger.save(mergedDocument, workspaceFile);
  }

  @Override
  public void importRepository(Path repositoryPath) {
    CommandletManager commandletManager = this.context.getCommandletManager();
    boolean buildDescriptorFound = false;
    for (Entry<Class<? extends LocalToolCommandlet>, String> entry : BUILD_TOOL_TO_IJ_TEMPLATE.entrySet()) {
      LocalToolCommandlet buildTool = commandletManager.getCommandlet(entry.getKey());
      Path buildDescriptor = buildTool.findBuildDescriptor(repositoryPath);
      if (buildDescriptor != null) {
        String templateFilename = entry.getValue();
        LOG.debug("Found build descriptor {} so merging template {}", buildDescriptor, templateFilename);
        mergeConfig(repositoryPath, templateFilename);
        buildDescriptorFound = true;
        break;
      }
    }
    if (!buildDescriptorFound) {
      LOG.warn("No supported build descriptor was found for project import in {}", repositoryPath);
    }

    importExtraToolInstallations(repositoryPath);
  }

  /**
   * Imports configured extra tool installations into IntelliJ for the repository contained in the given workspace.
   *
   * <p>
   * This method is used from repository import flow where only a repository path is known and the workspace must first be resolved from that repository
   * location.
   * </p>
   *
   * @param repositoryPath the repository path inside the workspace.
   */
  private void importExtraToolInstallations(Path repositoryPath) {
    Path workspacePath = getWorkspacePath(repositoryPath);
    importExtraToolInstallationsToWorkspace(workspacePath);
  }

  /**
   * Imports all configured and supported extra tool installations into IntelliJ workspace configuration.
   *
   * <p>
   * Extra tool installations are read from {@code settings/ide-extra-tools.json}. For each configured tool, IntelliJ import is only attempted if this class
   * provides IntelliJ-specific support via {@link #EXTRA_TOOL_CONFIGS}. This keeps the discovery of extra tools generic while the actual IDE import remains
   * explicit and controlled.
   * </p>
   *
   * @param workspacePath the IntelliJ workspace root whose configuration should be updated.
   */
  private void importExtraToolInstallationsToWorkspace(Path workspacePath) {
    ExtraTools extraTools = ExtraToolsMapper.get().loadJsonFromFolder(this.context.getSettingsPath());
    if (extraTools == null) {
      return;
    }

    for (String tool : extraTools.getSortedToolNames()) {
      IntellijExtraSdkConfig config = EXTRA_TOOL_CONFIGS.get(tool);
      if (config == null) {
        LOG.debug("Skipping IntelliJ import of extra tool '{}': no IntelliJ template support configured.", tool);
        continue;
      }
      List<ExtraToolInstallation> extraInstallations = extraTools.getExtraInstallations(tool);
      for (ExtraToolInstallation extraInstallation : extraInstallations) {
        mergeExtraToolInstallation(workspacePath, tool, extraInstallation, config);
      }
    }
  }

  /**
   * Merges a single extra IDEasy tool installation into IntelliJ workspace configuration.
   *
   * <p>
   * This method performs validation and variable preparation before delegating to the actual XML merge:
   * </p>
   * <ul>
   * <li>rejects reserved names that would collide with the main SDK/tool definition,</li>
   * <li>verifies that the extra tool installation really exists under {@code software/extra},</li>
   * <li>verifies that the required IntelliJ merge template is available.</li>
   * </ul>
   *
   * @param workspacePath the IntelliJ workspace root.
   * @param tool the IDEasy tool name such as {@code java}.
   * @param extraInstallation the configured extra installation from {@code ide-extra-tools.json}.
   * @param config the IntelliJ-specific import configuration for the tool.
   */
  private void mergeExtraToolInstallation(Path workspacePath, String tool, ExtraToolInstallation extraInstallation,
      IntellijExtraSdkConfig config) {

    String name = extraInstallation.name();
    if ((config.reservedName() != null) && config.reservedName().equalsIgnoreCase(name)) {
      LOG.warn("Skipping IntelliJ import for extra {} installation '{}': name conflicts with main {} installation.", tool, name, tool);
      return;
    }

    Path extraToolHome = this.context.getSoftwareExtraPath().resolve(tool).resolve(name);
    if (!Files.isDirectory(extraToolHome)) {
      LOG.warn("Skipping IntelliJ import for extra {} installation '{}': directory does not exist: {}", tool, name, extraToolHome);
      return;
    }

    Path templateFile = this.context.getSettingsPath()
        .resolve(TEMPLATE_LOCATION)
        .resolve(config.templateFile());
    if (!Files.exists(templateFile)) {
      throw new CliException(
          "Cannot import extra " + tool + " installation into IntelliJ: template file not found at " + templateFile + "\n"
              + "Please do an upstream merge of your settings git repository.");
    }

    ExtensibleEnvironmentVariables environmentVariables = new ExtensibleEnvironmentVariables(
        (AbstractEnvironmentVariables) this.context.getVariables().getParent(), this.context);
    environmentVariables.setValue("EXTRA_TOOL", tool);
    environmentVariables.setValue("EXTRA_NAME", name);
    environmentVariables.setValue("EXTRA_HOME", extraToolHome.toString().replace('\\', '/'));
    environmentVariables.setValue("EXTRA_VERSION", extraInstallation.version().toString());
    if (extraInstallation.edition() != null) {
      environmentVariables.setValue("EXTRA_EDITION", extraInstallation.edition());
    }

    mergeExtraToolInstallationToWorkspace(workspacePath, config, templateFile, environmentVariables.resolved());
  }

  /**
   * Performs the actual XML merge of an IntelliJ extra SDK/tool definition into the configured target file.
   *
   * @param workspacePath the IntelliJ workspace root.
   * @param config the IntelliJ-specific import configuration.
   * @param templateFile the resolved template file to merge from.
   * @param environmentVariables the resolved variables used to parameterize the template.
   */
  private void mergeExtraToolInstallationToWorkspace(Path workspacePath, IntellijExtraSdkConfig config, Path templateFile,
      EnvironmentVariables environmentVariables) {

    Path targetFile = workspacePath.resolve(config.targetFile());

    XmlMerger xmlMerger = new XmlMerger(this.context);
    XmlMergeDocument workspaceDocument = xmlMerger.load(targetFile);
    XmlMergeDocument templateDocument = xmlMerger.loadAndResolve(templateFile, environmentVariables);

    Document mergedDocument = xmlMerger.merge(templateDocument, workspaceDocument, false);

    xmlMerger.save(mergedDocument, targetFile);
  }
}
