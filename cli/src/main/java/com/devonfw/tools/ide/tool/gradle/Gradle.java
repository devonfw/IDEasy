package com.devonfw.tools.ide.tool.gradle;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;

import java.util.Set;

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
  public boolean install(EnvironmentContext environmentContext, boolean silent) {

    getCommandlet(Java.class).install(environmentContext);
    return super.install(environmentContext, silent);
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

}
