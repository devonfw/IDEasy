package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * An internal {@link Commandlet} to uninstall a tool.
 */
public class UninstallCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(UninstallCommandlet.class);

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
  protected void doRun() {

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
      if (toolCommandlet.isInstalled()) {
        toolCommandlet.uninstall();
      } else {
        removeSoftwareLeftover(toolCommandlet);
      }
    }
  }

  /**
   * Removes leftover software entries under {@link IdeContext#getSoftwarePath()} when the tool is no longer considered installed (e.g. broken symlink or
   * leftover folder with only a version file).
   *
   * @param toolCommandlet the {@link ToolCommandlet} to clean up.
   */
  private void removeSoftwareLeftover(ToolCommandlet toolCommandlet) {

    String tool = toolCommandlet.getName();
    // Use software/«tool» explicitly (not getToolPath()) so leftovers are cleaned even for package-manager tools
    // whose getToolPath() points to the parent tool.
    Path softwareToolPath = this.context.getSoftwarePath().resolve(tool);
    if (Files.exists(softwareToolPath, LinkOption.NOFOLLOW_LINKS)) {
      this.context.getFileAccess().delete(softwareToolPath);
      IdeLogLevel.SUCCESS.log(LOG, "Successfully uninstalled {}", tool);
    } else {
      LOG.warn("Couldn't uninstall {} because we could not find an installation", tool);
    }
  }
}
