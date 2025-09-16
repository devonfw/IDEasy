package com.devonfw.tools.ide.tool.corepack;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link NpmBasedCommandlet} for <a href="https://github.com/nodejs/corepack">corepack</a>.
 */
public class Corepack extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Corepack(IdeContext context) {

    super(context, "corepack", Set.of(Tag.TYPE_SCRIPT, Tag.BUILD));
  }
}
