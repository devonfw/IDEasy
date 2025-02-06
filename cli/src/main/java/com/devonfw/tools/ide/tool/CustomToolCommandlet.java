package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.repository.CustomToolMetadata;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
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
  public ToolRepository getToolRepository() {

    return this.context.getCustomToolRepository();
  }

  @Override
  public VersionIdentifier getConfiguredVersion() {

    return this.customTool.getVersion();
  }

  @Override
  public String getConfiguredEdition() {

    return this.customTool.getEdition();
  }

}
