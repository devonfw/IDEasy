package com.devonfw.tools.ide.tool.node;

import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://nodejs.org/">node</a>.
 */
public class Node extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Node(IdeContext context) {

    super(context, "node", Set.of(TAG_RUNTIME));
  }

}
