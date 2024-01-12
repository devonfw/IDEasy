package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
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

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public EditionGetCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
  }

  @Override
  public String getName() {

    return "get-edition";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    VersionIdentifier installedVersion = commandlet.getInstalledVersion();
    if (installedVersion == null) {
      throw new CliException("Tool " + commandlet.getName() + " is not installed!", 4);
    }

    String installedEdition = commandlet.getInstalledEdition();

    if (installedEdition == null) {
      this.context.warning("Couldn't get edition of installed tool {}.", getName());
      String configuredEdition = this.context.getVariables().getToolEdition(getName());
      this.context.info("Configured edition for tool {} is {}.", getName(), configuredEdition);
    } else {
      this.context.info(installedEdition);
    }
  }

}
