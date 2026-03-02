package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.Mvn;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeaBasedIdeToolCommandlet {

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
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {
    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    environmentContext.withEnvVar("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve(IDEA_PROPERTIES).toString());
  }

  private EnvironmentVariables getIntellijEnvironmentVariables(Path projectPath) {
    ExtensibleEnvironmentVariables environmentVariables = new ExtensibleEnvironmentVariables(
        (AbstractEnvironmentVariables) this.context.getVariables().getParent(), this.context);

    environmentVariables.setValue("PROJECT_PATH", projectPath.toString().replace('\\', '/'));
    return environmentVariables.resolved();
  }

  private void mergeConfig(Path repositoryPath, String configFilePath) {
    Path templatePath = this.context.getSettingsPath().resolve(TEMPLATE_LOCATION);
    Path templateFile = templatePath.resolve(configFilePath);
    if (!Files.exists(templateFile)) {
      throw new CliException(
          "Cannot import project into workspace: template file not found at " + templateFile + "\n"
              + "Please do an upstream merge of your settings git repository.");
    }
    Path workspacesPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES);
    Path workspacePath = this.context.getFileAccess().findAncestor(repositoryPath, workspacesPath, 1);
    if (workspacePath == null) {
      throw new CliException(
          "Cannot import project into workspace: could not find workspace from " + repositoryPath);
    }
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
    for (Entry<Class<? extends LocalToolCommandlet>, String> entry : BUILD_TOOL_TO_IJ_TEMPLATE.entrySet()) {
      LocalToolCommandlet buildTool = commandletManager.getCommandlet(entry.getKey());
      Path buildDescriptor = buildTool.findBuildDescriptor(repositoryPath);
      if (buildDescriptor != null) {
        String templateFilename = entry.getValue();
        this.context.debug("Found build descriptor {} so merging template {}", buildDescriptor, templateFilename);
        mergeConfig(repositoryPath, templateFilename);
        return;
      }
    }
    this.context.warning("No supported build descriptor was found for project import in {}", repositoryPath);
  }

}
