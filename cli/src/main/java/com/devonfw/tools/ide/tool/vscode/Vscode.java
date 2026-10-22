package com.devonfw.tools.ide.tool.vscode;

import static com.devonfw.tools.ide.version.VersionIdentifier.MAJOR_SEGMENT;

import java.io.IOException;
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
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;
import com.devonfw.tools.ide.tool.ide.AbstractIdeToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@link AbstractToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends AbstractIdeToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Vscode.class);

  /** The {@link #getConfiguredEdition() edition} for VSCodium. */
  private static final String EDITION_VSCODIUM = "vscodium";

  /** The {@link Path} of the legacy VSCode user-data folder relative to the workspace, still used by the workspace templates in the settings. */
  private static final Path LEGACY_USER_DATA = Path.of(".vscode", ".userdata");

  /** The name of the folder inside the VSCode user-data folder containing the user settings of the default profile. */
  private static final String FOLDER_USER = "User";

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
        step.success("Successfully installed plugin: {} with version: {}", plugin.name(), plugin.version());
      } else {
        step.success("Successfully installed plugin: {}", plugin.name());
      }
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

  @Override
  public int getIncompatibleVersionSegmentIndex() {

    return MAJOR_SEGMENT;
  }

  /**
   * Removes the legacy user-data folder from the workspace that VSCode does not read anymore (see #2142 and #2509). If the actual user-data folder does not yet
   * exist, the legacy folder is moved there to preserve its content, otherwise it is backed up.
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
    Path legacyUserData = workspaceFolder.resolve(LEGACY_USER_DATA);
    Path userData = getUserDataPath();
    if (isProfileEnabled()) {
      Path profilePath = getProfilePath();
      if (profilePath != null) {
        // redirects are applied to the already redirected parent, so the user settings are redirected from the user-data folder to the profile
        return Map.of(legacyUserData, userData, userData.resolve(FOLDER_USER), profilePath);
      }
    }
    return Map.of(legacyUserData, userData);
  }

  private boolean isProfileEnabled() {

    return Boolean.TRUE.equals(IdeVariables.VSCODE_PROFILE_ENABLED.get(this.context));
  }

  /**
   * @return the {@link Path} to the default VSCode user-data folder that VSCode uses if no {@code --user-data-dir} is given.
   */
  private Path getDefaultUserDataPath() {

    String folderName = EDITION_VSCODIUM.equals(getConfiguredEdition()) ? "VSCodium" : "Code";
    Path userHome = this.context.getUserHome();
    SystemInfo systemInfo = this.context.getSystemInfo();
    if (systemInfo.isWindows()) {
      return userHome.resolve("AppData").resolve("Roaming").resolve(folderName);
    } else if (systemInfo.isMac()) {
      return userHome.resolve("Library").resolve("Application Support").resolve(folderName);
    }
    return userHome.resolve(".config").resolve(folderName);
  }

  /**
   * @return the {@link Path} to the folder of the {@link #getProfileName() IDEasy profile} inside the {@link #getDefaultUserDataPath() default user-data
   *     folder} or {@code null} if VSCode has not yet created that profile. VSCode stores the profiles with a generated id in its {@code storage.json} so the
   *     folder is only known after VSCode created the profile (see #2471 and #2568).
   */
  private Path getProfilePath() {

    Path userPath = getDefaultUserDataPath().resolve(FOLDER_USER);
    Path storageFile = userPath.resolve("globalStorage").resolve("storage.json");
    if (!Files.isRegularFile(storageFile)) {
      return null;
    }
    String profileName = getProfileName();
    try {
      JsonNode profiles = JsonMapping.create().readTree(storageFile.toFile()).path("userDataProfiles");
      for (JsonNode profile : profiles) {
        if (profileName.equals(profile.path("name").asText())) {
          return userPath.resolve("profiles").resolve(profile.path("location").asText());
        }
      }
    } catch (IOException e) {
      LOG.warn("Failed to read VSCode profiles from {}", storageFile, e);
      return null;
    }
    LOG.info("VSCode profile {} does not exist yet, user settings will be applied to it with the next start of VSCode.", profileName);
    return null;
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
