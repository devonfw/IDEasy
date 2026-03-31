package com.devonfw.tools.ide.tool.kotlinc;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://kotlinlang.org/docs/native-overview.html">Kotlin Native</a> (kotlincnative).
 */
public class KotlincNative extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public KotlincNative(IdeContext context) {

    super(context, "kotlinc-native", Set.of(Tag.KOTLIN, Tag.RUNTIME));
  }
}
