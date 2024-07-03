package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
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

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public VersionGetCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
  }

  @Override
  public String getName() {

    return "get-version";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    VersionIdentifier installedVersion = commandlet.getInstalledVersion();
    if (installedVersion == null) {
      this.context.info("The configured version for tool {} is {}", commandlet.getName(),
          commandlet.getConfiguredVersion());
      this.context.info("To install that version call the following command:");
      this.context.info("ide install {}", commandlet.getName());
      return;
    }
    this.context.info(installedVersion.toString());
  }

}
