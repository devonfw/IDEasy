package com.devonfw.tools.ide.tool.npm;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.node.Node;

/**
 * {@link ToolCommandlet} for <a href="https://nodejs.org/">node</a>.
 */

public class Npm extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Npm(IdeContext context) {

    super(context, "npm", Set.of(Tag.JAVA_SCRIPT, Tag.RUNTIME));
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Node.class).install();
    return super.install(silent);
  }
}
