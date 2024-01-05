package com.devonfw.tools.ide.tool.quarkus;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://quarkus.io/">Quarkus</a>.
 */
public class Quarkus extends LocalToolCommandlet {
  /**
   * The constructor
   *
   * @param context the {@link IdeContext}
   */
  public Quarkus(IdeContext context) {

    super(context, "quarkus", Set.of(Tag.JAVA, Tag.FRAMEWORK));
  }

}
