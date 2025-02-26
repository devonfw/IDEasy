package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * An internal {@link Commandlet} to uninstall a tool.
 */
public class UninstallCommandlet extends Commandlet {

  /** The tool to uninstall. */
  public final ToolProperty tools;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UninstallCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tools = add(new ToolProperty("", false, true, "tool"));
  }

  @Override
  public String getName() {

    return "uninstall";
  }

  @Override
  public boolean isIdeRootRequired() {

    return this.tools.getValueCount() > 0;
  }

  @Override
  public void run() {

    int valueCount = this.tools.getValueCount();
    if (valueCount == 0) {
      if (!this.context.isForceMode()) {
        this.context.askToContinue("Sub-command uninstall without any further arguments will perform the entire uninstallation of IDEasy.\n"
            + "Since this is typically not to be called manually, you may have forgotten to specify the tool to install as extra argument.\n"
            + "The current command will uninstall IDEasy from your computer. Are you sure?");
      }
      IdeasyCommandlet ideasy = new IdeasyCommandlet(this.context);
      ideasy.uninstallIdeasy();
      return;
    }
    for (int i = 0; i < valueCount; i++) {
      ToolCommandlet toolCommandlet = this.tools.getValue(i);
      if (toolCommandlet.getInstalledVersion() != null) {
        toolCommandlet.uninstall();
      } else {
        this.context.warning("Couldn't uninstall " + toolCommandlet.getName() + " because we could not find an installation");
      }

    }
  }
}
