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
import com.devonfw.tools.ide.environment.EnvironmentVariablesIntellijImport;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.merge.xml.XmlMergeDocument;
import com.devonfw.tools.ide.merge.xml.XmlMerger;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeaBasedIdeToolCommandlet {

  private static final String IDEA = "idea";

  private static final String IDEA64_EXE = IDEA + "64.exe";

  private static final String IDEA_BASH_SCRIPT = IDEA + ".sh";

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
    environmentContext.withEnvVar("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve("idea.properties").toString());
  }

  private void mergeMisc(Path repositoryPath) throws IOException {
    Path workspaceMiscPath = getOrCreateWorkspaceXmlFile(repositoryPath, "misc.xml");

    XmlMerger xmlMerger = new XmlMerger(context);
    Path templateMiscPath = this.context.getSettingsTemplatePath().resolve("conf/mvn/misc.xml");

    EnvironmentVariables environmentVariables = new EnvironmentVariablesIntellijImport((AbstractEnvironmentVariables) context.getVariables(), repositoryPath);

    XmlMergeDocument workspaceMisc = xmlMerger.load(workspaceMiscPath);
    XmlMergeDocument mavenTemplateMisc = xmlMerger.loadAndResolve(templateMiscPath, environmentVariables);

    Document mergedMisc = xmlMerger.merge(mavenTemplateMisc, workspaceMisc, false);

    xmlMerger.save(mergedMisc, workspaceMiscPath);
  }

  private void mergeGradle(Path repositoryPath) throws IOException {
    Path workspaceGradlePath = getOrCreateWorkspaceXmlFile(repositoryPath, "gradle.xml");

    XmlMerger xmlMerger = new XmlMerger(this.context);
    Path templateGradleXmlPath = this.context.getSettingsTemplatePath().resolve("conf/gradle/gradle.xml");

    EnvironmentVariables environmentVariables = new EnvironmentVariablesIntellijImport((AbstractEnvironmentVariables) context.getVariables(), repositoryPath);
    XmlMergeDocument workspaceGradleXml = xmlMerger.load(workspaceGradlePath);
    XmlMergeDocument gradleTemplateXml = xmlMerger.loadAndResolve(templateGradleXmlPath, environmentVariables);

    Document mergedMisc = xmlMerger.merge(gradleTemplateXml, workspaceGradleXml, false);

    xmlMerger.save(mergedMisc, workspaceGradlePath);
  }

  private Path getOrCreateWorkspaceXmlFile(Path repositoryPath, String fileName) {
    Path ideaPath = repositoryPath.resolve("../.idea");
    Path workspaceFilePath = ideaPath.resolve(fileName);
    FileAccess fileAccess = new FileAccessImpl(context);
    if (!fileAccess.isFile(workspaceFilePath)) {
      fileAccess.mkdirs(ideaPath);
      fileAccess.touch(workspaceFilePath);
      // xml merger fails when merging an empty file
      // this should probably be fixed in the xml merger, but for now here is a workaround:
      fileAccess.writeFileContent("""
          <?xml version="1.0" encoding="UTF-8"?>
          <project version="4">
          </project>
          """, workspaceFilePath);
    }
    return workspaceFilePath;
  }

  @Override
  public void importRepository(Path repositoryPath) {
    System.out.println("Repo path:" + repositoryPath);
    // check if pom.xml exists
    Path pomPath = repositoryPath.resolve("pom.xml");
    if (Files.exists(pomPath)) {
      try {
        mergeMisc(repositoryPath);
      } catch (IOException e) {
        this.context.error(e);
      }

    } else {
      this.context.debug("no pom.xml found");
    }

    // check if build.gradle exists
    Path javaGradlePath = repositoryPath.resolve("build.gradle");
    Path kotlinGradlePath = repositoryPath.resolve("build.gradle.kts");
    if (Files.exists(javaGradlePath) || Files.exists(kotlinGradlePath)) {
      try {
        mergeGradle(repositoryPath);
      } catch (IOException e) {
        this.context.error(e);
      }
    } else {
      this.context.debug("no build.gradle found!");
    }
  }

}
