package com.devonfw.tools.ide.tool.vscode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ide.AbstractIdeToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * {@link AbstractToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends AbstractIdeToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Vscode.class);

  /** The {@link #getConfiguredEdition() edition} for VSCodium. */
  private static final String EDITION_VSCODIUM = "vscodium";

  /** The {@link Path} of the legacy VSCode user-data folder relative to the workspace, still used by the workspace templates in the settings. */
  private static final Path LEGACY_USER_DATA = Path.of(".vscode", ".userdata");

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

    if (isProfileEnabled()) {
      // A named profile keeps its own list of plugins outside of IDE_HOME that survives a reset of the plugins folder. VS Code trusts that list, answers
      // "already installed" with exit code 0 and installs nothing, even with --force. Uninstalling first cleans up the list (see #2504).
      uninstallPluginFromProfile(plugin, pc);
    }
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

  /**
   * Uninstalls the given plugin from the {@link #getProfileName() profile}. Fails harmlessly if the plugin is not installed.
   *
   * @param plugin the {@link ToolPluginDescriptor} to uninstall.
   * @param pc the {@link ProcessContext} to use.
   */
  private void uninstallPluginFromProfile(ToolPluginDescriptor plugin, ProcessContext pc) {

    ProcessContext uninstallPc = pc.createChild().errorHandling(ProcessErrorHandling.NONE);
    ProcessResult result = runTool(uninstallPc, ProcessMode.DEFAULT_CAPTURE, List.of("--uninstall-extension", plugin.id()));
    if (!result.isSuccessful()) {
      LOG.debug("Plugin {} was not installed in VSCode profile {} before its installation.", plugin.id(), getProfileName());
    }
  }

  /**
   * @return {@code true} if the feature toggle {@link IdeVariables#VSCODE_PROFILE_ENABLED} is enabled and VSCode uses a named {@link #getProfileName()
   *     profile}, {@code false} otherwise.
   */
  private boolean isProfileEnabled() {

    return Boolean.TRUE.equals(IdeVariables.VSCODE_PROFILE_ENABLED.get(this.context));
  }

  /**
   * @return the name of the VSCode profile used to isolate settings, extension state and authentication per IDEasy project and workspace.
   */
  private String getProfileName() {

    return "ideasy-" + this.context.getProjectName() + "-" + this.context.getWorkspaceName();
  }

  /**
   * @return the {@link Path} to the VSCode user-data folder passed via {@code --user-data-dir}.
   */
  private Path getUserDataPath() {

    return getIdeMetadataPath().resolve("config");
  }

  @Override
  public void configureWorkspace() {

    cleanupLegacyUserData();
    super.configureWorkspace();
  }

  /**
   * Removes the legacy user-data folder from the workspace that VSCode does not read anymore (see #2142 and #2509). If the actual user-data folder does not
   * yet exist, the legacy folder is moved there to preserve its content, otherwise it is backed up.
   */
  private void cleanupLegacyUserData() {

    Path legacyUserData = this.context.getWorkspacePath().resolve(LEGACY_USER_DATA);
    if (!Files.isDirectory(legacyUserData)) {
      return;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    Path userData = getUserDataPath();
    if (Files.exists(userData)) {
      LOG.warn("Removing obsolete VSCode user-data folder {} from workspace since VSCode uses {}", legacyUserData, userData);
      fileAccess.backup(legacyUserData);
    } else {
      LOG.info("Moving VSCode user-data folder {} out of workspace to {}", legacyUserData, userData);
      fileAccess.mkdirs(userData.getParent());
      fileAccess.move(legacyUserData, userData);
    }
  }

  @Override
  public Map<Path, Path> getWorkspaceRedirects(Path workspaceFolder) {

    // the settings still provide the user settings template in the legacy location inside the workspace
    return Map.of(workspaceFolder.resolve(LEGACY_USER_DATA), getUserDataPath());
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, List<String> args) {

    if (this.context.getSystemInfo().isWsl()) {
      pc.withEnvVar("DONT_PROMPT_WSL_INSTALL", "1");
    }
    pc.addArg("--new-window");
    if (isProfileEnabled()) {
      // Use a named profile (not --user-data-dir) so VS Code keeps its IPC lock at the default location.
      // This lets the OS-level vscode:// protocol handler (OAuth callbacks e.g. GitHub/Copilot) find the
      // already-running IDEasy window. Each project and workspace gets its own profile for isolated auth and settings.
      pc.addArg("--profile=" + getProfileName());
    } else {
      pc.addArg("--user-data-dir=" + getUserDataPath());
    }
    Path vsCodeExtensionFolder = this.context.getIdeHome().resolve("plugins/vscode");
    pc.addArg("--extensions-dir=" + vsCodeExtensionFolder);
    pc.addArg(this.context.getWorkspacePath());
    super.configureToolArgs(pc, processMode, args);
  }

}
