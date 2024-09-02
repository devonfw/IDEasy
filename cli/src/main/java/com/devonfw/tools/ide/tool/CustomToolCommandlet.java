package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.repo.CustomTool;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for a {@link CustomTool}.
 */
public class CustomToolCommandlet extends LocalToolCommandlet {

  private CustomTool customTool;

  public CustomToolCommandlet(IdeContext context, CustomTool customTool) {

    super(context, customTool.getTool(), null);
    this.customTool = customTool;
  }

  @Override
  public ToolInstallation installTool(VersionIdentifier version, EnvironmentContext environmentContext) {

    return installTool(version, this.customTool.getEdition(), environmentContext);
  }

  @Override
  public ToolInstallation installTool(VersionIdentifier version) {

    return installTool(version, this.customTool.getEdition(), null);
  }

  @Override
  public ToolInstallation installTool(VersionIdentifier version, String edition, EnvironmentContext environmentContext) {

    return installTool(version, edition, this.context.getCustomToolRepository(), environmentContext);
  }


  @Override
  public VersionIdentifier getConfiguredVersion() {

    return this.customTool.getVersion();
  }

}
