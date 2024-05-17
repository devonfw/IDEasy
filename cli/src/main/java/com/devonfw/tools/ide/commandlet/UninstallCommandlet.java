package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * An internal {@link Commandlet} to uninstall a tool.
 */
public class UninstallCommandlet extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UninstallCommandlet(IdeContext context) {

    super(context, "uninstall", Set.of(Tag.UNINSTALL));
  }

  @Override
  public String getName() {

    return "uninstall";
  }

  @Override
  public void run() {

    String[] userInputArray = this.arguments.asArray();

    for (String toolName : userInputArray) {
      try {
        String commandletName = this.context.getCommandletManager().getToolCommandlet(toolName).getName();
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

