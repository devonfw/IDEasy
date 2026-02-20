package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * An internal {@link Commandlet} to list versions for a tool.
 *
 * @see ToolCommandlet#listVersions()
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
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
  }

  @Override
  public String getName() {

    return "list-versions";
  }

  @Override
  protected void doRun() {

    ToolCommandlet commandlet = this.tool.getValue();
    commandlet.listVersions();
  }

}
