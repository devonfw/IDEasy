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
    IdeSubLogger logger = this.context.level(IdeLogLevel.PROCESSABLE);
    if (this.installed.isTrue() && !this.configured.isTrue()) {// get installed version
      VersionIdentifier installedVersion = commandlet.getInstalledVersion();
      if (installedVersion == null) {
        toolInstallInfo(commandlet.getName(), configuredVersion, null, commandlet);
      } else {
        logger.log(installedVersion.toString());
      }
    } else if (!this.installed.isTrue() && this.configured.isTrue()) {// get configured version
      logger.log(configuredVersion.toString());
    } else { // get both configured and installed version
      VersionIdentifier installedVersion = commandlet.getInstalledVersion();
      if (configuredVersion.matches(installedVersion)) {
        logger.log(installedVersion.toString());
      } else {
        toolInstallInfo(commandlet.getName(), configuredVersion, installedVersion, commandlet);
      }
    }
  }

  private void toolInstallInfo(String toolName, VersionIdentifier configuredVersion, VersionIdentifier installedVersion, ToolCommandlet commandlet) {

    if (installedVersion == null) {
      this.context.info("No installation of tool {} was found.", commandlet.getName());
    } else {
      this.context.info("The installed version for tool {} is {}", commandlet.getName(), installedVersion);
    }
    this.context.info("The configured version for tool {} is {}", toolName, configuredVersion);
    this.context.info("To install that version call the following command:");
    this.context.info("ide install {}", toolName);

  }

}
