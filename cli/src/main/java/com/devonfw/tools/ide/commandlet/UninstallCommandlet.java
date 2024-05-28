package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.nio.file.Files;
import java.nio.file.Path;

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

      try {
        String commandletName = toolCommandlet.getName();
        Path softwarePath = context.getSoftwarePath().resolve(commandletName);
        if (Files.exists(softwarePath)) {
          try {
            context.getFileAccess().delete(softwarePath);
            this.context.success("Successfully uninstalled " + commandletName);
          } catch (Exception e) {
            this.context.error("Couldn't uninstall " + commandletName);
          }
        } else {
          this.context.warning("An installed version of " + commandletName + " does not exist");
        }
      } catch (Exception e) {
        this.context.error(e.getMessage());
      }
    }
  }
}
