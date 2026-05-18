package com.devonfw.tools.ide.tool.spyder;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;

public class Spyder extends PipBasedCommandlet {

  public Spyder(IdeContext context) {
    super(context, "spyder", Set.of(Tag.SPYDER, Tag.IDE));
  }

  @Override
  public ToolInstallation install(ToolInstallRequest request) {

    PackageManagerRequest pmRequest =
        new PackageManagerRequest("install", "spyder");

    runPackageManager(pmRequest);

    return null;
  }

  @Override
  public String getToolHelpArguments() {
    return "--help";
  }

}
