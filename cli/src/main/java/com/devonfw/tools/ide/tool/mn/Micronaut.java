package com.devonfw.tools.ide.tool.mn;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractToolCommandlet} for <a href="https://micronaut.io/">Micronaut CLI</a> ({@code mn}).
 */
public class Micronaut extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Micronaut(IdeContext context) {

    super(context, "mn", Set.of(Tag.MICRONAUT));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
