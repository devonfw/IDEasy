package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.merge.xml.XmlMergeDocument;
import com.devonfw.tools.ide.merge.xml.XmlMerger;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallation;
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
    registerExtraSdkTemplate("java", Path.of(".intellij/config/options/jdk.table.xml"));
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
    environmentContext.withEnvVar("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve(IDEA_PROPERTIES).toString());
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
            Notice: You have configured IDEasy to use the IntelliJ Ultimate Edition.
            Since version 2025.3, the Ultimate and Community editions of IntelliJ have been unified into a single edition.
            Since you are attempting to install a version of IntelliJ that is 2025.3 or newer,
            we are automatically switching your edition to the unified edition to ensure compatibility.
            To specifically install the last true ultimate version of IntelliJ, please run "ide install intellij 2025.2.6.1".
            Otherwise, we recommend permanently switching to the unified edition by running "ide set-edition intellij intellij".""");
        edition = new ToolEdition(this.tool, "intellij");
        requested.replaceEdition(edition);
      }
    }
    return requested;
  }

  @Override
  protected String getTemplateFolder() {

    return FOLDER_IDEA_CONFIG;
  }

  /**
   * Merges the IntelliJ project template into the workspace's {@code .idea} config (e.g. {@code misc.xml} or {@code gradle.xml}).
   *
   * @param templateFile the resolved {@link Path} to the template file in the settings repository.
   * @param workspaceFile the {@link Path} to the target {@code .idea} file to merge into.
   * @param environmentVariables the {@link EnvironmentVariables} to resolve variables (e.g. {@code PROJECT_PATH}) in the template.
   */
  @Override
  protected void doMergeTemplate(Path templateFile, Path workspaceFile, EnvironmentVariables environmentVariables) {

    XmlMerger xmlMerger = new XmlMerger(this.context);
    XmlMergeDocument workspaceDocument = xmlMerger.load(workspaceFile);
    XmlMergeDocument templateDocument = xmlMerger.loadAndResolve(templateFile, environmentVariables);

    Document mergedDocument = xmlMerger.merge(templateDocument, workspaceDocument, false);

    xmlMerger.save(mergedDocument, workspaceFile);
  }

  @Override
  protected Map<Class<? extends LocalToolCommandlet>, String> getBuildTool2TemplateMap() {

    return BUILD_TOOL_TO_IJ_TEMPLATE;
  }
}
