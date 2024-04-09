package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.repo.CustomTool;
import com.devonfw.tools.ide.version.VersionIdentifier;


public class CustomToolCommandlet extends LocalToolCommandlet {

  private CustomTool customTool;

  public CustomToolCommandlet(IdeContext context, CustomTool customTool) {

   super(context, customTool.getTool(), null);
   this.customTool = customTool;
  }

  @Override
  public ToolInstallation installInRepo(VersionIdentifier version) {

    return installInRepo(version, this.customTool.getEdition());
  }

 @Override
  public ToolInstallation installInRepo(VersionIdentifier version, String edition) {

    return installInRepo(version, edition, this.context.getCustomToolRepository());
  }

  @Override
  public VersionIdentifier getConfiguredVersion() {

    return this.customTool.getVersion();
  }
}
