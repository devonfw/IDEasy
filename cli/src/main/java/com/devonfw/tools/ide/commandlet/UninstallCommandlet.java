package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.ToolProperty;

/**
 * An internal {@link Commandlet} to uninstall a tool.
 */
public class UninstallCommandlet extends Commandlet {

  /** The tool to uninstall. */
  public final ToolProperty tool;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UninstallCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
  }

  @Override
  public String getName() {

    return "uninstall";
  }

  @Override
  public void run() {

    String commandletName = this.tool.getValue().getName();
    Path softwarePath = context.getSoftwarePath().resolve(commandletName);
    if (Files.exists(softwarePath)) {
      FileAccess fileAccess = context.getFileAccess();
      try {
        fileAccess.delete(softwarePath);
        this.context.success("Successfully uninstalled " + commandletName);
      } catch (Exception e) {
        throw new IllegalStateException("Couldn't uninstall " + commandletName);
      }
    } else {
      this.context.info("An installed version of " + commandletName + " does not exist");
    }
  }
}
