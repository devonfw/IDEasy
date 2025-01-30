package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.repository.CustomToolMetadata;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for a {@link CustomToolMetadata}.
 */
public class CustomToolCommandlet extends LocalToolCommandlet {

  private CustomToolMetadata customTool;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param customTool the {@link CustomToolMetadata} to handle (e.g. install or uninstall).
   */
  public CustomToolCommandlet(IdeContext context, CustomToolMetadata customTool) {

    super(context, customTool.getTool(), null);
    this.customTool = customTool;
  }

  @Override
  public VersionIdentifier getConfiguredVersion() {

    return this.customTool.getVersion();
  }

  @Override
  public String getConfiguredEdition() {

    return this.customTool.getEdition();
  }

  @Override
  public ToolInstallation installTool(GenericVersionRange version, EnvironmentContext environmentContext, String edition) {

    return installTool(version, environmentContext, edition, this.context.getCustomToolRepository());
  }
}
