package com.devonfw.tools.ide.tool.copilot;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractToolCommandlet} for <a href="https://github.com/github/copilot-cli">GitHub Copilot CLI</a>.
 */
public class Copilot extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Copilot(IdeContext context) {

    super(context, "copilot", Set.of(Tag.ARTIFICIAL_INTELLIGENCE));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
