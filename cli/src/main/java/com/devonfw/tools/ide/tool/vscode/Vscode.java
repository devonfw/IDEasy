package com.devonfw.tools.ide.tool.vscode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliProcessException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
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

  /** The maximum number of attempts to detect the {@link #getProfileName() profile} after VS Code has been launched. */
  private static final int PROFILE_DETECTION_ATTEMPTS = 30;

  /** The delay in milliseconds between two attempts to detect the {@link #getProfileName() profile}. */
  private static final long PROFILE_DETECTION_DELAY = 1000L;

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

    if (isProfileEnabled() && !isProfileAvailable(pc)) {
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
    if (waitForProfile(pc)) {
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
    } else {
      IdeLogLevel.WARNING.log(LOG, "VS Code did not create the profile {} in time so the plugins could not be installed. They will be installed the next "
          + "time you run 'ide vscode'.", getProfileName());
    }
  }

  /**
   * @param pc the {@link ProcessContext} to use.
   * @return {@code true} if the {@link #getProfileName() profile} exists (eventually), {@code false} if it did not show up in time.
   */
  private boolean waitForProfile(ProcessContext pc) {

    for (int attempt = 1; attempt <= PROFILE_DETECTION_ATTEMPTS; attempt++) {
      if (isProfileAvailable(pc)) {
        return true;
      }
      LOG.debug("Waiting for VS Code to create the profile {} ({}/{}).", getProfileName(), attempt, PROFILE_DETECTION_ATTEMPTS);
      try {
        Thread.sleep(PROFILE_DETECTION_DELAY);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  /**
   * @param pc the {@link ProcessContext} to use.
   * @return {@code true} if the {@link #getProfileName() profile} already exists in VS Code, {@code false} otherwise.
   */
  private boolean isProfileAvailable(ProcessContext pc) {

    // there is no CLI command to create or query a profile, but every profile bound call fails while the profile does not exist
    try {
      return runTool(pc, ProcessMode.DEFAULT_CAPTURE, List.of("--list-extensions")).isSuccessful();
    } catch (CliProcessException e) {
      LOG.debug("The VS Code profile {} does not exist yet.", getProfileName(), e);
      return false;
    }
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

}
