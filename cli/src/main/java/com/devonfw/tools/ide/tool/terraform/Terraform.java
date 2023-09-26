package com.devonfw.tools.ide.tool.terraform;

import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for terraform CLI (terraform).
 */
public class Terraform extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Terraform(IdeContext context) {

    super(context, "terraform", Set.of(TAG_BUILD));

  }

}
