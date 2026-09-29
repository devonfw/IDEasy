package com.devonfw.tools.ide.tool.gh;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractToolCommandlet} for github CLI (gh).
 */
public class Gh extends AbstractLocalToolCommandlet {

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
