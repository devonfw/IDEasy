package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

import static com.devonfw.tools.ide.process.ProcessResult.TOOL_NOT_INSTALLED;

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
      throw new CliException("Tool " + commandlet.getName() + " is not installed!", TOOL_NOT_INSTALLED);
    }

    try {
      String installedEdition = commandlet.getInstalledEdition();
      this.context.info(installedEdition);
    } catch (IllegalStateException e) {
      String configuredEdition = this.context.getVariables().getToolEdition(getName());
      this.context.info("The configured edition for tool {} is {}.", getName(), configuredEdition);

    }
  }

}
