package com.devonfw.tools.ide.tool.vscode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
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

  @Override
  protected String getTemplateFolder() {

    return FOLDER_VSCODE;
  }

  /**
   * Merges the VSCode settings template into the workspace's {@code .vscode/settings.json}.
   *
   * @param templateFile the resolved {@link Path} to the settings template in the settings repository.
   * @param workspaceFile the {@link Path} to the workspace {@code settings.json} to merge into.
   * @param environmentVariables the {@link EnvironmentVariables} to resolve variables (e.g. {@code PROJECT_PATH}) in the template.
   */
  @Override
  protected void doMergeTemplate(Path templateFile, Path workspaceFile, EnvironmentVariables environmentVariables) {

    // Ensure .vscode folder exists
    this.context.getFileAccess().mkdirs(workspaceFile.getParent());

    // Merge template into workspace settings (template acts as "setup" for creation, also as "update" for variable resolution)
    JsonMerger jsonMerger = new JsonMerger(this.context);
    jsonMerger.merge(templateFile, templateFile, environmentVariables, workspaceFile);
    LOG.debug("Merged VSCode settings into {}", workspaceFile);
  }

}
