package com.devonfw.tools.ide.tool.kotlinc;

import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for kotlincnative CLI (kotlincnative).
 */
public class KotlincNative extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public KotlincNative(IdeContext context) {

    super(context, "kotlincnative", Set.of(TAG_CLOUD, TAG_IAC));
  }
}