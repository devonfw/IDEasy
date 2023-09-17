package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * An internal {@link Commandlet} to set a tool version.
 *
 * @see ToolCommandlet#setVersion(VersionIdentifier, boolean)
 */
public class VersionListCommandlet extends Commandlet {

  /** The tool to list the versions of. */
  public final ToolProperty tool;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public VersionListCommandlet(IdeContext context) {

    super(context);
    addKeyword("list");
    this.tool = add(new ToolProperty("", true, "tool"));
    addKeyword("version");
  }

  @Override
  public String getName() {

    return "list-version";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    commandlet.listVersions();
  }

}
