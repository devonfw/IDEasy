package com.devonfw.tools.ide.tool.kotlinc;

import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://kotlinlang.org/docs/command-line.html">Kotlin command-line compiler</a> (kotlinc).
 */
public class Kotlinc extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Kotlinc(IdeContext context) {

    super(context, "kotlinc", Set.of(TAG_JAVA, TAG_RUNTIME));
  }
}
