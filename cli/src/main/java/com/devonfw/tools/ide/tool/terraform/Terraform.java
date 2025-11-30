package com.devonfw.tools.ide.tool.terraform;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

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
  protected void postInstallOnNewInstallation(ToolInstallRequest request) {

    super.postInstallOnNewInstallation(request);
    runTool(request, ProcessMode.DEFAULT, "-install-autocomplete");
  }

  @Override
  public String getToolHelpArguments() {

    return "-help";
  }
}
