package com.devonfw.tools.ide.tool.squirrelsql;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for SQuirreL SQL Client.
 */
public class SquirrelSql extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public SquirrelSql(IdeContext context) {

    super(context, "squirrelsql", Set.of(Tag.DB));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
