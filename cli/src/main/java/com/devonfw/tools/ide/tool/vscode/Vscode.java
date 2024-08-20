package com.devonfw.tools.ide.tool.vscode;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginDescriptor;

/**
 * {@link ToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends IdeToolCommandlet {

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

    return "code";
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {
    Path binaryPath = Path.of(getBinaryName());
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(binaryPath);
    pc.addArg("--install-extension");
    pc.addArg(plugin.getId());
    pc.run();
  }


}
