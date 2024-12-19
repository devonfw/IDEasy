package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * An internal {@link Commandlet} to get the installed edition for a tool.
 *
 * @see ToolCommandlet#getInstalledEdition()
 */
public class EditionGetCommandlet extends Commandlet {

  /** The tool to get the edition of. */
  public final ToolProperty tool;

  /** Flag to get the configured version. */
  public final FlagProperty configured;

  /** Flag to get the installed version. */
  public final FlagProperty installed;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public EditionGetCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.configured = add(new FlagProperty("--configured"));
    this.installed = add(new FlagProperty("--installed"));
  }

  @Override
  public String getName() {

    return "get-edition";
  }

  @Override
  public boolean isProcessableOutput() {

    return true;
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    String configuredEdition = commandlet.getConfiguredEdition();
    String installedEdition = commandlet.getInstalledEdition();
    IdeSubLogger logger = this.context.level(IdeLogLevel.PROCESSABLE);
    if (this.installed.isTrue() && !this.configured.isTrue()) { // get installed edition
      if (commandlet.getInstalledVersion() == null) {
        // note: getInstalledEdition() will fallback to configured edition and not return null, therefore we use getInstalledVersion()
        toolInstallInfo(commandlet.getName(), configuredEdition, null, commandlet);
      } else {
        logger.log(installedEdition);
      }
    } else if (!this.installed.isTrue() && this.configured.isTrue()) { // get configured edition
      logger.log(configuredEdition);
    } else if (this.installed.isTrue() && this.configured.isTrue()) { // get both configured and installed edition
      logger.log(configuredEdition);
      if (!configuredEdition.equals(installedEdition)) {
        if (commandlet.getInstalledVersion() != null) {
          logger.log(installedEdition);
        } else {
          logger.log("No installed edition detected");
        }
      }
    } else { // get configured or installed depending on if the tool is installed or not
      if (commandlet.getInstalledVersion() == null) {
        logger.log(configuredEdition);
      } else {
        logger.log(installedEdition);
      }
    }
  }

  private void toolInstallInfo(String toolName, String configuredEdition, String installedEdition, ToolCommandlet commandlet) {

    IdeSubLogger logger = this.context.level(IdeLogLevel.PROCESSABLE);
    if (installedEdition == null) {
      logger.log("No installation of tool {} was found.", commandlet.getName());
    } else {
      logger.log("The installed edition for tool {} is {}", commandlet.getName(), installedEdition);
    }
    logger.log("The configured edition for tool {} is {}", toolName, configuredEdition);
    logger.log("To install that edition call the following command:");
    logger.log("ide install {}", toolName);
  }
}
