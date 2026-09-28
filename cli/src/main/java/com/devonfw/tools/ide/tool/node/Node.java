package com.devonfw.tools.ide.tool.node;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://nodejs.org/">node</a>.
 */
public class Node extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Node.class);

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

    LOG.info("For a list of supported options and arguments, use \"node --help\"");
  }
}
