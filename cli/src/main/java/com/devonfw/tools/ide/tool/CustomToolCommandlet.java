package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.repo.CustomTool;
import com.devonfw.tools.ide.version.VersionIdentifier;

public class CustomToolCommandlet extends LocalToolCommandlet {

  private CustomTool customTool;

  public CustomToolCommandlet(IdeContext context, CustomTool customTool) {

    super(context, customTool.getTool(), null);
    this.customTool = customTool;
  }

  @Override
  public ToolInstallation installInRepo(EnvironmentContext environmentContext, VersionIdentifier version) {

    return installInRepo(environmentContext, version, this.customTool.getEdition());
  }

  @Override
  public ToolInstallation installInRepo(EnvironmentContext environmentContext, VersionIdentifier version, String edition) {

    return installInRepo(environmentContext, version, edition, this.context.getCustomToolRepository());
  }

  @Override
  public VersionIdentifier getConfiguredVersion() {

    return this.customTool.getVersion();
  }
}
