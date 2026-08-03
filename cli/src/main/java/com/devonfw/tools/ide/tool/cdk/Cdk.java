package com.devonfw.tools.ide.tool.cdk;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://docs.aws.amazon.com/cdk/v2/guide/home.html">AWS CDK</a>.
 */
public class Cdk extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Cdk(IdeContext context) {

    super(context, "cdk", Set.of(Tag.IAC, Tag.CLOUD));
  }

  @Override
  public String getPackageName() {

    return "aws-cdk";
  }
}
