package com.devonfw.tools.ide.tool.npm;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link NpmBasedCommandlet} for <a href="https://www.npmjs.com/">npm</a>.
 */
public class Npm extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Npm(IdeContext context) {

    super(context, "npm", Set.of(Tag.JAVA_SCRIPT, Tag.BUILD));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}
