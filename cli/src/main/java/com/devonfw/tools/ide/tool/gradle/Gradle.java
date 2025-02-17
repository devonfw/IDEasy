package com.devonfw.tools.ide.tool.gradle;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://gradle.org/">gradle</a>.
 */
public class Gradle extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Gradle(IdeContext context) {

    super(context, "gradle", Set.of(Tag.JAVA, Tag.BUILD));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

}
