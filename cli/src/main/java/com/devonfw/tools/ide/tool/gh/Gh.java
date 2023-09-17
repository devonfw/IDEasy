package com.devonfw.tools.ide.tool.gh;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for github CLI (gh).
 */
public class Gh extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Gh(IdeContext context) {

    super(context, "gh");
  }

}
