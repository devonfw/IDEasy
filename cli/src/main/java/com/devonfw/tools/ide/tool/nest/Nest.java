package com.devonfw.tools.ide.tool.nest;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://docs.nestjs.com/cli/overview">Nest CLI</a>.
 */
public class Nest extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Nest(IdeContext context) {

    super(context, "nest", Set.of(Tag.NEST, Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  @Override
  public String getPackageName() {

    return "@nestjs/cli";
  }
}
