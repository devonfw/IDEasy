package com.devonfw.tools.ide.tool.flutter;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractLocalToolCommandlet} for the Flutter SDK (including the bundled Dart SDK).
 */
public class Flutter extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Flutter(IdeContext context) {

    super(context, "flutter", Set.of(Tag.FLUTTER));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
