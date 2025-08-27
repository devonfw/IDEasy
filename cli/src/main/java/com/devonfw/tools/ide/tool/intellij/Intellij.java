package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.w3c.dom.Document;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
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
    Path workspaceMiscPath = getWorkspaceMiscPath(repositoryPath);

    XmlMerger xmlMerger = new XmlMerger(context);
    Path templateMiscPath = this.context.getSettingsTemplatePath().resolve("conf/mvn/misc.xml");

    XmlMergeDocument workspaceMisc = xmlMerger.load(workspaceMiscPath);
    XmlMergeDocument mavenTemplateMisc = xmlMerger.load(templateMiscPath);

    Document mergedMisc = xmlMerger.merge(mavenTemplateMisc, workspaceMisc, false);

    xmlMerger.save(mergedMisc, workspaceMiscPath);
  }

  private Path getWorkspaceMiscPath(Path repositoryPath) {
    Path ideaPath = repositoryPath.resolve("../.idea");
    Path workspaceMiscPath = ideaPath.resolve("misc.xml");
    FileAccess fileAccess = new FileAccessImpl(context);
    if (!fileAccess.isFile(workspaceMiscPath)) {
      fileAccess.mkdirs(ideaPath);
      fileAccess.touch(workspaceMiscPath);
      // xml merger fails when merging an empty file
      // this should probably be fixed in the xml merger, but for now here is a workaround:
      fileAccess.writeFileContent("""
          <?xml version="1.0" encoding="UTF-8"?>
          <project version="4">
            <component name="ProjectRootManager" version="2" languageLevel="JDK_21" default="true" project-jdk-name="Java" project-jdk-type="JavaSDK"/>
          </project>
          """, workspaceMiscPath);
    }
    return workspaceMiscPath;
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
    Path gradlePath = repositoryPath.resolve("build.gradle");
    if (Files.exists(gradlePath)) {
      this.context.debug("build.grade found!");
    } else {
      this.context.debug("no build.gradle found!");
    }
  }

}
