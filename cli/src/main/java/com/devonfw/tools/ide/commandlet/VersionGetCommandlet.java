package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * An internal {@link Commandlet} to get the installed version for a tool.
 *
 * @see ToolCommandlet#getInstalledVersion()
 */
public class VersionGetCommandlet extends Commandlet {

  /** The tool to get the version of. */
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
  public VersionGetCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.configured = add(new FlagProperty("--configured"));
    this.installed = add(new FlagProperty("--installed"));
  }

  @Override
  public String getName() {

    return "get-version";
  }

  @Override
  public boolean isProcessableOutput() {

    return true;
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    VersionIdentifier configuredVersion = commandlet.getConfiguredVersion();
    VersionIdentifier installedVersion = commandlet.getInstalledVersion();
    IdeSubLogger logger = this.context.level(IdeLogLevel.PROCESSABLE);
    if (this.installed.isTrue() && !this.configured.isTrue()) {// get installed version
      if (installedVersion == null) {
        toolInstallInfo(commandlet.getName(), configuredVersion, null, commandlet);
      } else {
        logger.log(installedVersion.toString());
      }
    } else if (!this.installed.isTrue() && this.configured.isTrue()) {// get configured version
      logger.log(configuredVersion.toString());
    } else if (this.installed.isTrue() && this.configured.isTrue()) {// get both configured and installed version
      logger.log(configuredVersion.toString());
      if (!configuredVersion.matches(installedVersion)) {
        if (installedVersion != null) {
          logger.log(installedVersion.toString());
        } else {
          logger.log("No installed version detected");
        }
      }
    } else {
      if (installedVersion == null) {
        logger.log(configuredVersion.toString());
      } else {
        logger.log(installedVersion.toString());
      }
    }
  }

  private void toolInstallInfo(String toolName, VersionIdentifier configuredVersion, VersionIdentifier installedVersion, ToolCommandlet commandlet) {

    IdeSubLogger logger = this.context.level(IdeLogLevel.PROCESSABLE);
    if (installedVersion == null) {
      logger.log("No installation of tool {} was found.", commandlet.getName());
    } else {
      logger.log("The installed version for tool {} is {}", commandlet.getName(), installedVersion);
    }
    logger.log("The configured version for tool {} is {}", toolName, configuredVersion);
    logger.log("To install that version call the following command:");
    logger.log("ide install {}", toolName);

  }

}
