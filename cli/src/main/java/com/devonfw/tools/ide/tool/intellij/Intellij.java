package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.w3c.dom.Document;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.ExtensibleEnvironmentVariables;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.merge.xml.XmlMergeDocument;
import com.devonfw.tools.ide.merge.xml.XmlMerger;
import com.devonfw.tools.ide.process.EnvironmentContext;
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

  private static final String TEMPLATE_LOCATION = "intellij/workspace/repository/.idea";
  private static final String GRADLE_XML = "gradle.xml";
  private static final String MISC_XML = "misc.xml";
  private static final String IDEA_PROPERTIES = "idea.properties";

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
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve(IDEA_PROPERTIES).toString());
  }

  private EnvironmentVariables getIntellijEnvironmentVariables(Path projectPath) {
    ExtensibleEnvironmentVariables environmentVariables = new ExtensibleEnvironmentVariables(
        (AbstractEnvironmentVariables) this.context.getVariables().getParent(), this.context);

    environmentVariables.addVariableResolver("PROJECT_PATH", projectPath.toString());
    return environmentVariables.resolved();
  }

  private void mergeConfig(Path repositoryPath, String configName) throws IOException {
    Path workspacePath = getOrCreateWorkspaceXmlFile(repositoryPath, configName);

    if (workspacePath != null) {
      XmlMerger xmlMerger = new XmlMerger(context);
      Path templatePath = this.context.getSettingsPath().resolve(TEMPLATE_LOCATION).resolve(configName);

      EnvironmentVariables environmentVariables = getIntellijEnvironmentVariables(repositoryPath.getFileName());

      if (!Files.exists(workspacePath)) {
        xmlMerger.createValidEmptyXmlFile("project", workspacePath);
      }
      XmlMergeDocument workspaceDocument = xmlMerger.load(workspacePath);
      XmlMergeDocument templateDocument = xmlMerger.loadAndResolve(templatePath, environmentVariables);

      Document mergedDocument = xmlMerger.merge(templateDocument, workspaceDocument, false);

      xmlMerger.save(mergedDocument, workspacePath);
    }
  }

  private Path getOrCreateWorkspaceXmlFile(Path repositoryPath, String fileName) {
    FileAccess fileAccess = new FileAccessImpl(context);

    Path ideaParentPath = fileAccess
        .findAncestorWithFolder(repositoryPath, "." + IDEA, "workspaces");

    if (ideaParentPath != null) {
      return ideaParentPath.resolve("." + IDEA).resolve(fileName);
    }
    return null;
  }

  private boolean importTemplatesExist() {
    Path templatePath = this.context.getSettingsPath().resolve(TEMPLATE_LOCATION);
    Path miscXml = templatePath.resolve(MISC_XML);
    Path gradleXml = templatePath.resolve(GRADLE_XML);
    return Files.exists(miscXml) && Files.exists(gradleXml);
  }

  @Override
  public void importRepository(Path repositoryPath) {
    if (!importTemplatesExist()) {
      this.context.warning("Could not automatically import repository due to missing template files.");
      return;
    }
    // check if pom.xml exists
    Path pomPath = repositoryPath.resolve(Mvn.POM_XML);
    if (Files.exists(pomPath)) {
      try {
        mergeConfig(repositoryPath, MISC_XML);
      } catch (IOException e) {
        this.context.error(e);
      }

    } else {
      this.context.debug("no pom.xml found was found in {}", pomPath);
    }

    // check if build.gradle exists
    Path javaGradlePath = repositoryPath.resolve(Gradle.BUILD_GRADLE);
    Path kotlinGradlePath = repositoryPath.resolve(Gradle.BUILD_GRADLE_KTS);
    if (Files.exists(javaGradlePath) || Files.exists(kotlinGradlePath)) {
      try {
        mergeConfig(repositoryPath, GRADLE_XML);
      } catch (IOException e) {
        this.context.error(e);
      }
    } else {
      this.context.debug("no build.gradle found in {} and {}", javaGradlePath, kotlinGradlePath);
    }
  }

}
