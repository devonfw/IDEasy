package com.devonfw.tools.ide.tool.node;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.nls.NlsBundle;
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

    super(context, "node", Set.of(Tag.JAVA_SCRIPT, Tag.RUNTIME));
  }

  @Override
  public void printHelp(NlsBundle bundle) {

    this.context.info("For a list of supported options and arguments, use \"node --help\"");
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }
}
