package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
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
    this.configured = add(new FlagProperty("--configured", false, null));
    this.installed = add(new FlagProperty("--installed", false, null));
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

    if (this.installed.isTrue() && !this.configured.isTrue()) {// get installed version

      VersionIdentifier installedVersion = commandlet.getInstalledVersion();
      if (installedVersion == null) {
        this.context.info("No installation of tool {} was found.", commandlet.getName());
        toolInstallInfo(commandlet.getName(), configuredVersion);
      }
      this.context.info(installedVersion.toString());

    } else if (!this.installed.isTrue() && this.configured.isTrue()) {// get configured version

      this.context.info(configuredVersion.toString());

    } else { // get both configured and installed version

      VersionIdentifier installedVersion = commandlet.getInstalledVersion();
      if (configuredVersion.compareVersion(installedVersion).isEqual()) {
        this.context.info(installedVersion.toString());
      } else {
        if (installedVersion == null) {
          this.context.info("No installation of tool {} was found.", commandlet.getName());
        } else {
          this.context.info("The installed version for tool {} is {}", commandlet.getName(), installedVersion);
        }
        toolInstallInfo(commandlet.getName(), configuredVersion);
      }

    }

  }

  private void toolInstallInfo(String toolName, VersionIdentifier configuredVersion) {

    this.context.info("The configured version for tool {} is {}", toolName, configuredVersion);
    this.context.info("To install that version call the following command:");
    this.context.info("ide install {}", toolName);

  }

}
