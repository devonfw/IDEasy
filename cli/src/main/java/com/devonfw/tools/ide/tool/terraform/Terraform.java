package com.devonfw.tools.ide.tool.terraform;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.util.Set;

/**
 * {@link ToolCommandlet} for terraform CLI (terraform).
 */
public class Terraform extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Terraform(IdeContext context) {

    super(context, "terraform", Set.of(Tag.IAC));
  }

  @Override
  protected void postInstall() {

    super.postInstall();
    runTool(ProcessMode.DEFAULT, null, "-install-autocomplete");
  }

  @Override
  public String getHelpCommand() {

    return "-help";
  }
}
