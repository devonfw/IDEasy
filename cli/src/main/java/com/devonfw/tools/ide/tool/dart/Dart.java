package com.devonfw.tools.ide.tool.dart;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractLocalToolCommandlet} for the Dart SDK (programming language).
 */
public class Dart extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Dart(IdeContext context) {

    super(context, "dart", Set.of(Tag.DART));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
