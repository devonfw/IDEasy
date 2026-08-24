package com.devonfw.tools.ide.tool.vscode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.ExtensibleEnvironmentVariables;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.merge.JsonMerger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * {@link ToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends IdeToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Vscode.class);

  /** The {@link #getConfiguredEdition() edition} for VSCodium. */
  private static final String EDITION_VSCODIUM = "vscodium";

  /** Folder name for VSCode workspace configuration. */
  private static final String FOLDER_VSCODE = ".vscode";

  /** Settings file name for VSCode. */
  private static final String SETTINGS_JSON = "settings.json";

  /** Template location for VSCode repository workspace settings. */
  private static final String TEMPLATE_LOCATION = "vscode/workspace/repository/" + FOLDER_VSCODE;

  /** Map of build tool classes to their corresponding VSCode settings template. */
  private static final Map<Class<? extends LocalToolCommandlet>, String> BUILD_TOOL_TO_TEMPLATE =
      Map.of(Mvn.class, SETTINGS_JSON, Gradle.class, SETTINGS_JSON);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Vscode(IdeContext context) {

    super(context, "vscode", Set.of(Tag.VS_CODE));
  }

  @Override
  protected String getBinaryName() {

    if (EDITION_VSCODIUM.equals(getConfiguredEdition())) {
      return "codium";
    }
    return "code";
  }


  @Override
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {

    List<String> extensionsCommands = new ArrayList<>();
    extensionsCommands.add("--force");
    extensionsCommands.add("--install-extension");
    String extensionInstallTarget = plugin.id();
    // If a version number was specified, add it to the extension identifier with the format "extensionId@version"
    boolean versionSpecified = (plugin.version() != null) && !plugin.version().isBlank();
    if (versionSpecified) {
      extensionInstallTarget = extensionInstallTarget + "@" + plugin.version();
    }
    extensionsCommands.add(extensionInstallTarget);
    ProcessResult result = runTool(pc, ProcessMode.DEFAULT_CAPTURE, extensionsCommands);
    if (result.isSuccessful()) {
      if (versionSpecified) {
        IdeLogLevel.SUCCESS.log(LOG, "Successfully installed plugin: {} with version: {}", plugin.name(), plugin.version());
      } else {
        IdeLogLevel.SUCCESS.log(LOG, "Successfully installed plugin: {}", plugin.name());
      }
      step.success();
      return true;
    }
    if (versionSpecified) {
      IdeLogLevel.ERROR.log(LOG, "Failed to install plugin: {} with version: {}", plugin.name(), plugin.version());
    } else {
      IdeLogLevel.ERROR.log(LOG, "Failed to install plugin: {}", plugin.name());
    }
    return false;
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, List<String> args) {

    if (this.context.getSystemInfo().isWsl()) {
      pc.withEnvVar("DONT_PROMPT_WSL_INSTALL", "1");
    }
    Path vsCodeConf = this.context.getWorkspacePath().resolve(".vscode/.userdata");
    pc.addArg("--new-window");
    pc.addArg("--user-data-dir=" + vsCodeConf);
    Path vsCodeExtensionFolder = this.context.getIdeHome().resolve("plugins/vscode");
    pc.addArg("--extensions-dir=" + vsCodeExtensionFolder);
    pc.addArg(this.context.getWorkspacePath());
    super.configureToolArgs(pc, processMode, args);
  }

  @Override
  protected Map<Class<? extends LocalToolCommandlet>, String> getBuildTool2TemplateMap() {

    return BUILD_TOOL_TO_TEMPLATE;
  }

  /**
   * Creates {@link EnvironmentVariables} for resolving VSCode workspace templates with the relative project path.
   *
   * @param projectPath the relative {@link Path} from workspace to repository.
   * @return the resolved {@link EnvironmentVariables}.
   */
  private EnvironmentVariables getVscodeEnvironmentVariables(Path projectPath) {
    ExtensibleEnvironmentVariables environmentVariables = new ExtensibleEnvironmentVariables(
        (AbstractEnvironmentVariables) this.context.getVariables().getParent(), this.context);

    environmentVariables.setValue("PROJECT_PATH", projectPath.toString().replace('\\', '/'));
    return environmentVariables.resolved();
  }

  /**
   * Merges the VSCode settings template into the workspace's {@code .vscode/settings.json}.
   *
   * @param repositoryPath the {@link Path} to the repository to import.
   * @param configFilePath the filename of the config file (e.g. {@code settings.json}).
   */
  @Override
  protected void mergeTemplate(Path repositoryPath, String configFilePath) {
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
    JsonMerger jsonMerger = new JsonMerger(this.context);
    EnvironmentVariables environmentVariables = getVscodeEnvironmentVariables(workspacePath.relativize(repositoryPath));
    Path vscodeFolder = workspacePath.resolve(FOLDER_VSCODE);
    Path workspaceFile = vscodeFolder.resolve(configFilePath);

    // Ensure .vscode folder exists
    this.context.getFileAccess().mkdirs(vscodeFolder);

    // Merge template into workspace settings (template acts as "setup" for creation, also as "update" for variable resolution)
    jsonMerger.merge(templateFile, templateFile, environmentVariables, workspaceFile);

    LOG.debug("Merged VSCode settings into {} for repository at {}", workspaceFile, repositoryPath);
  }

}
