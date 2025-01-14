package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.repo.CustomToolMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for a {@link CustomToolMetadata}.
 */
public class CustomToolCommandlet extends LocalToolCommandlet {

  private CustomToolMetadata customTool;

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

}
