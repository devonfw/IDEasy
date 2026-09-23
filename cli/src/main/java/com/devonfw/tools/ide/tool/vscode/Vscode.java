package com.devonfw.tools.ide.tool.vscode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * {@link ToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends IdeToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Vscode.class);

  /** The {@link #getConfiguredEdition() edition} for VSCodium. */
  private static final String EDITION_VSCODIUM = "vscodium";

  /**
   * The maximum time in milliseconds to wait for VS Code to create the {@link #getProfileName() profile} after it has been launched. In practice the profile
   * shows up within a few seconds, this is only a safety net for slow machines (e.g. a first start that is delayed by a virus scanner).
   */
  private static final long PROFILE_CREATION_TIMEOUT = 20_000L;

  /** The delay in milliseconds between two checks whether the {@link #getProfileName() profile} has been created. */
  private static final long PROFILE_CREATION_POLL_DELAY = 1000L;

  private Collection<ToolPluginDescriptor> deferredPlugins;

  private ProcessContext deferredPluginProcessContext;

  private boolean installingIntoNewProfile;

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

  /**
   * @return the name of the VSCode profile used to isolate settings, extensions and UI state per IDEasy project and workspace.
   */
  private String getProfileName() {

    return "ideasy-" + this.context.getProjectName() + "-" + this.context.getWorkspaceName();
  }

  /**
   * @return {@code true} if VS Code is isolated via a named {@code --profile}, {@code false} for the legacy {@code --user-data-dir} behaviour.
   */
  private boolean isProfileEnabled() {

    return Boolean.TRUE.equals(IdeVariables.VSCODE_PROFILE_ENABLED.get(this.context));
  }

  @Override
  protected boolean isForcePluginInstallation() {

    // A profile that VSCode has just created is empty. Our plugin marker files live per project while the plugins of VSCode belong to a profile, so the marker
    // files cannot tell whether a plugin is present in the new profile. Without this the user ends up with an empty IDE. See issue #2471.
    return this.installingIntoNewProfile || super.isForcePluginInstallation();
  }

  @Override
  protected int installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {

    if (isProfileEnabled() && (getProfileState() == ProfileState.MISSING)) {
      // VS Code only creates a profile when it opens a window. CLI calls such as --install-extension merely look the profile up and abort with
      // "Profile '«name»' not found". Therefore the plugins can only be installed after VS Code has been launched. See issue #2471.
      LOG.info("The VS Code profile {} does not exist yet so the plugins are installed after VS Code has been started.", getProfileName());
      this.deferredPlugins = plugins;
      this.deferredPluginProcessContext = pc;
      return 0;
    }
    return super.installPlugins(plugins, pc);
  }

  @Override
  public ProcessResult runTool(ToolInstallRequest request, ProcessMode processMode, List<String> args) {

    ProcessResult result = super.runTool(request, processMode, args);
    installDeferredPlugins();
    return result;
  }

  /**
   * Installs the plugins that have been deferred by {@link #installPlugins(Collection, ProcessContext)} because the {@link #getProfileName() profile} did not
   * exist before VS Code was launched.
   */
  private void installDeferredPlugins() {

    Collection<ToolPluginDescriptor> plugins = this.deferredPlugins;
    ProcessContext pc = this.deferredPluginProcessContext;
    this.deferredPlugins = null;
    this.deferredPluginProcessContext = null;
    if ((plugins == null) || plugins.isEmpty()) {
      return;
    }
    ProfileState profileState = waitForProfile();
    if (profileState == ProfileState.AVAILABLE) {
      this.installingIntoNewProfile = true;
      int installedPlugins;
      try {
        installedPlugins = super.installPlugins(plugins, pc);
      } finally {
        this.installingIntoNewProfile = false;
      }
      if (installedPlugins > 0) {
        IdeLogLevel.INTERACTION.log(LOG, "The plugins have been installed after VS Code was started. Please reload the window to activate them.");
      }
    } else if (profileState == ProfileState.MISSING) {
      IdeLogLevel.WARNING.log(LOG, "VS Code did not create the profile {} within {} seconds so the plugins could not be installed. They will be installed the "
          + "next time you run 'ide vscode'.", getProfileName(), PROFILE_CREATION_TIMEOUT / 1000);
    } else {
      IdeLogLevel.WARNING.log(LOG, "The plugins could not be installed into the VS Code profile {} since VS Code failed (see above).", getProfileName());
    }
  }

  /**
   * Waits until VS Code has created the {@link #getProfileName() profile} after it has been launched, at most {@link #PROFILE_CREATION_TIMEOUT}.
   *
   * @return the final {@link ProfileState}. {@link ProfileState#MISSING} if the profile did not show up in time, {@link ProfileState#UNKNOWN} if VS Code
   *     failed for another reason what makes waiting pointless.
   */
  private ProfileState waitForProfile() {

    long deadline = System.currentTimeMillis() + PROFILE_CREATION_TIMEOUT;
    boolean waitingLogged = false;
    while (true) {
      ProfileState profileState = getProfileState();
      if ((profileState != ProfileState.MISSING) || (System.currentTimeMillis() >= deadline)) {
        return profileState;
      }
      if (!waitingLogged) {
        LOG.info("Waiting up to {} seconds for VS Code to create the profile {}...", PROFILE_CREATION_TIMEOUT / 1000, getProfileName());
        waitingLogged = true;
      }
      try {
        Thread.sleep(PROFILE_CREATION_POLL_DELAY);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return ProfileState.MISSING;
      }
    }
  }

  /**
   * @return the {@link ProfileState} of the {@link #getProfileName() profile} as reported by VS Code.
   */
  private ProfileState getProfileState() {

    // There is no CLI command to create or query a profile, but every profile bound call fails while the profile does not exist.
    // A missing profile is an expected state here, so we use our own process context that does not log or throw on a non-zero exit code.
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE);
    this.context.setEnvironmentOfInstalledTools(pc);
    ProcessResult result = runTool(pc, ProcessMode.DEFAULT_CAPTURE, List.of("--list-extensions"));
    if (result.isSuccessful()) {
      return ProfileState.AVAILABLE;
    }
    // VS Code reports "Profile '«name»' not found." on stderr. We only match the quoted profile name since the message itself may be localized.
    String quotedProfileName = "'" + getProfileName() + "'";
    for (String line : result.getErr()) {
      if (line.contains(quotedProfileName)) {
        LOG.debug("The VS Code profile {} does not exist yet.", getProfileName());
        return ProfileState.MISSING;
      }
    }
    IdeLogLevel.WARNING.log(LOG, "Could not determine if the VS Code profile {} exists since VS Code failed with exit code {}.", getProfileName(),
        result.getExitCode());
    result.log(IdeLogLevel.WARNING);
    return ProfileState.UNKNOWN;
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
      // already-running IDEasy window. Each project and workspace gets its own profile for isolated settings, extensions and UI state.
      // Note that a profile does NOT isolate authentication: VS Code keeps the auth sessions in the OS keyring and shares them across all profiles.
      pc.addArg("--profile=" + getProfileName());
    } else {
      pc.addArg("--user-data-dir=" + getIdeMetadataPath().resolve("config"));
    }
    Path vsCodeExtensionFolder = this.context.getIdeHome().resolve("plugins/vscode");
    pc.addArg("--extensions-dir=" + vsCodeExtensionFolder);
    pc.addArg(this.context.getWorkspacePath());
    super.configureToolArgs(pc, processMode, args);
  }

  /**
   * The state of the {@link #getProfileName() VS Code profile}.
   */
  private enum ProfileState {

    /** The profile exists. */
    AVAILABLE,

    /** The profile does not exist (yet). */
    MISSING,

    /** VS Code failed for another reason (e.g. a crash or a broken installation), so the state of the profile is unknown. */
    UNKNOWN
  }

}
