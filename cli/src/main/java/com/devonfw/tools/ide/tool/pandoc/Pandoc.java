package com.devonfw.tools.ide.tool.pandoc;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractToolCommandlet} for <a href="https://pandoc.org/">Pandoc</a>, a universal document converter.
 */
public class Pandoc extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Pandoc(IdeContext context) {

    super(context, "pandoc", Set.of(Tag.DOCUMENTATION));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
