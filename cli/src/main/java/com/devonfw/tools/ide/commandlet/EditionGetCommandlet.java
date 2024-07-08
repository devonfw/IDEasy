package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

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
    this.configured = add(new FlagProperty("--configured", false, null));
    this.installed = add(new FlagProperty("--installed", false, null));
  }

  @Override
  public String getName() {

    return "get-edition";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    String configuredEdition = commandlet.getConfiguredEdition();

    if (this.installed.isTrue() && !this.configured.isTrue()) { // get installed edition

      VersionIdentifier installedVersion = commandlet.getInstalledVersion();
      if (installedVersion == null) {
        this.context.info("No installation of tool {} was found.", commandlet.getName());
        toolInstallInfo(commandlet.getName(), configuredEdition);
      } else {
        String installedEdition = commandlet.getInstalledEdition();
        this.context.info(installedEdition);
      }

    } else if (!this.installed.isTrue() && this.configured.isTrue()) { // get configured edition

      this.context.info(configuredEdition);

    } else { // get both configured and installed edition
      String installedEdition = commandlet.getInstalledEdition();

      if (configuredEdition.equals(installedEdition)) {
        this.context.info(installedEdition);
      } else {
        if (installedEdition == null) {
          this.context.info("No installation of tool {} was found.", commandlet.getName());
        } else {
          this.context.info("The installed edition for tool {} is {}", commandlet.getName(), installedEdition);
        }
        toolInstallInfo(commandlet.getName(), configuredEdition);
      }

    }

  }

  private void toolInstallInfo(String toolName, String configuredEdition) {

    this.context.info("The configured edition for tool {} is {}", toolName, configuredEdition);
    this.context.info("To install that edition call the following command:");
    this.context.info("ide install {}", toolName);
  }
}
