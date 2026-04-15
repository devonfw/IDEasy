package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
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

  private static final Logger LOG = LoggerFactory.getLogger(Intellij.class);

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
        LOG.debug("Found build descriptor {} so merging template {}", buildDescriptor, templateFilename);
        mergeConfig(repositoryPath, templateFilename);
        return;
      }
    }
    LOG.warn("No supported build descriptor was found for project import in {}", repositoryPath);
  }

  @Override
  public ProcessResult runTool(ProcessContext pc, ProcessMode processMode, List<String> args) {

    String userVmArgsContent = this.context.getVariables().get("INTELLIJ_VM_ARGS");
    if (userVmArgsContent == null || userVmArgsContent.isEmpty()) {
      return super.runTool(pc, processMode, args);
    }
    String[] userVmArgs = userVmArgsContent.trim().split("\\s+");

    Path defaultVmOptionsPath = resolveDefaultVmOptionsPath(this.getToolPath());
    String defaultVmArgsContent = this.context.getFileAccess().readFileContent(defaultVmOptionsPath);
    if (defaultVmArgsContent == null || defaultVmArgsContent.isEmpty()) {
      LOG.debug("Default intellij jvm options not found at: {}", defaultVmOptionsPath);
      return super.runTool(pc, processMode, args);
    }
    String[] defaultVmArgs = defaultVmArgsContent.trim().split("\\s+");

    Path confPath = this.context.getWorkspacePath().resolve(".idea.vmoptions");
    this.context.getFileAccess().writeFileContent(mergeVmArgs(defaultVmArgs, userVmArgs), confPath, true);

    pc.withEnvVar("IDEA_VM_OPTIONS", confPath.toAbsolutePath().toString());
    return super.runTool(pc, processMode, args);
  }

  private Path resolveDefaultVmOptionsPath(Path softwarePath) {

    if (this.context.getSystemInfo().isWindows()) {
      return softwarePath
          .resolve("bin")
          .resolve("idea64.exe.vmoptions");
    }
    //if (this.context.getSystemInfo().isMac()) {
    //  return softwarePath
    //      .resolve("..")
    //      .resolve("bin")
    //      .resolve("idea.vmoptions");
    //} TODO: Mac sym links are linked "too deep"

    return softwarePath // Linux
        .resolve("bin")
        .resolve("idea64.vmoptions");
  }

  private String extractJvmOptionsKey(String arg) {

    // Only options with clear override semantics are merged.
    // All other JVM arguments are treated as atomic flags by design.
    if (arg.startsWith("-Xmx")) {
      return "-Xmx";
    }
    if (arg.startsWith("-Xms")) {
      return "-Xms";
    }
    if (arg.startsWith("-XX:") || arg.startsWith("-D")) {
      int eq = arg.indexOf('=');
      return eq > 0 ? arg.substring(0, eq) : arg;
    }

    return arg;
  }

  private boolean sameJvmKey(String a, String b) {

    return extractJvmOptionsKey(a).equals(extractJvmOptionsKey(b));
  }

  private String mergeVmArgs(String[] defaults, String[] userArgs) {

    List<String> result = new ArrayList<>(defaults.length + userArgs.length);
    Collections.addAll(result, defaults);
    for (String userArg : userArgs) {
      boolean replaced = false;
      for (int i = 0; i < result.size(); i++) {
        if (sameJvmKey(result.get(i), userArg)) {
          result.set(i, userArg); //override default arg with user defined arg
          replaced = true;
          break;
        }
      }
      if (!replaced) { // in case of arg does not exist in default options
        result.add(userArg);
      }
    }

    return String.join(System.lineSeparator(), result);
  }
}
