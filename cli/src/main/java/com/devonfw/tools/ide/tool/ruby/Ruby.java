package com.devonfw.tools.ide.tool.ruby;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for the Ruby programming language.
 */
public class Ruby extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Ruby(IdeContext context) {

    super(context, "ruby", Set.of(Tag.RUBY));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
