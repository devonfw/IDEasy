package com.devonfw.tools.ide.tool.nestjs;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://docs.nestjs.com/cli/overview">NestJS CLI</a>.
 */
public class NestJs extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public NestJs(IdeContext context) {

    super(context, "nestjs", Set.of(Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  @Override
  public String getPackageName() {

    return "@nestjs/cli";
  }
}
