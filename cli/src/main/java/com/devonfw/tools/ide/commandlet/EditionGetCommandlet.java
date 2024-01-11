package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * An internal {@link Commandlet} to set a tool version.
 *
 * @see ToolCommandlet#setVersion(VersionIdentifier, boolean)
 */
public class EditionGetCommandlet extends Commandlet {

  /** The tool to get the version of. */
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

    // i dont want to get the set edition but from the tool which is installed.

//    this.context.getVariables().getToolEdition(this.tool.getName());
//    String edition = commandlet.getEdition();
//    VersionIdentifier installedVersion = commandlet.getInstalledVersion();
//    if (installedVersion == null) {
//      throw new CliException("Tool " + commandlet.getName() + " is not installed!", 4);
//    }
//    this.context.info(installedVersion.toString());
  }

}
