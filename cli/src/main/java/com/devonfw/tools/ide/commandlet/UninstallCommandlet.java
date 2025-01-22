package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
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
    this.tools = add(new ToolProperty("", true, true, "tool"));
  }

  @Override
  public String getName() {

    return "uninstall";
  }

  @Override
  public void run() {

    for (int i = 0; i < this.tools.getValueCount(); i++) {
      ToolCommandlet toolCommandlet = this.tools.getValue(i);
      if (toolCommandlet.getInstalledVersion() != null) {
        toolCommandlet.uninstall();
      } else {
        this.context.warning("Couldn't uninstall " + toolCommandlet.getName() + " because we could not find an installation");
      }

    }
  }
}
