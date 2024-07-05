package com.devonfw.tools.ide.tool.gh;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.util.Set;

/**
 * {@link ToolCommandlet} for github CLI (gh).
 */
public class Gh extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Gh(IdeContext context) {

    super(context, "gh", Set.of(Tag.CLOUD));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}
