package com.devonfw.tools.ide.tool.vscode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * {@link ToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends IdeToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Vscode.class);

  /** The {@link #getConfiguredEdition() edition} for VSCodium. */
  public static final String EDITION_VSCODIUM = "vscodium";

  /** Plugin IDs collected during {@link #installPlugins} that the VSCodium build refused to install. */
  private final List<String> vscodiumUnavailablePlugins = new ArrayList<>();

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
  protected void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {
    boolean isVscodium = EDITION_VSCODIUM.equals(getConfiguredEdition());
    if (isVscodium) {
      this.vscodiumUnavailablePlugins.clear();
      // VSCodium uses open-vsx and is missing some plugins. Bypass the per-plugin Step framework
      // (no "Start: ..." / "ended ..." logs) and silence per-process errors; we report failures once at the end.
      pc.errorHandling(ProcessErrorHandling.NONE);
    }
    this.context.runWithoutLogging(() -> {
      IdeProgressBar pb = this.context.newProgressBarForPlugins(plugins.size());
      pc.setOutputListener((msg, err) -> {
        if (msg.contains("Installing extension ")) {
          pb.stepBy(1);
        }
      });
      if (isVscodium) {
        installPluginsSilently(plugins, pc);
      } else {
        super.installPlugins(plugins, pc);
      }
      pb.close();
    });
    if (isVscodium && !this.vscodiumUnavailablePlugins.isEmpty()) {
      LOG.warn("{} plugin(s) could not be installed on the VSCodium open-source build, "
              + "either because they are Microsoft-proprietary or are not published to open-vsx:\n  - {}\n"
              + "For full plugin support, set VSCODE_EDITION=vscode to use Microsoft's distribution.",
          this.vscodiumUnavailablePlugins.size(),
          String.join("\n  - ", this.vscodiumUnavailablePlugins));
    }
  }

  private void installPluginsSilently(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {
    for (ToolPluginDescriptor plugin : plugins) {
      if (!plugin.active()) {
        continue;
      }
      Path markerFile = retrievePluginMarkerFilePath(plugin);
      boolean markerExists = markerFile != null && Files.exists(markerFile);
      if (markerExists && !this.context.isForcePlugins()) {
        continue;
      }
      List<String> args = new ArrayList<>();
      args.add("--force");
      args.add("--install-extension");
      args.add(plugin.id());
      ProcessResult result = runTool(pc, ProcessMode.DEFAULT_CAPTURE, args);
      if (result.isSuccessful()) {
        createPluginMarkerFile(plugin);
      } else {
        this.vscodiumUnavailablePlugins.add(plugin.id());
      }
    }
  }

  @Override
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {

    List<String> extensionsCommands = new ArrayList<>();
    extensionsCommands.add("--force");
    extensionsCommands.add("--install-extension");
    extensionsCommands.add(plugin.id());
    ProcessResult result = runTool(pc, ProcessMode.DEFAULT_CAPTURE, extensionsCommands);
    if (result.isSuccessful()) {
      IdeLogLevel.SUCCESS.log(LOG, "Successfully installed plugin: {}", plugin.name());
      step.success();
      return true;
    }
    LOG.warn("An error occurred while installing plugin: {}", plugin.name());
    return false;
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, List<String> args) {

    Path vsCodeConf = this.context.getWorkspacePath().resolve(".vscode/.userdata");
    pc.addArg("--new-window");
    pc.addArg("--user-data-dir=" + vsCodeConf);
    Path vsCodeExtensionFolder = this.context.getIdeHome().resolve("plugins/vscode");
    pc.addArg("--extensions-dir=" + vsCodeExtensionFolder);
    pc.addArg(this.context.getWorkspacePath());
    super.configureToolArgs(pc, processMode, args);
  }

}
