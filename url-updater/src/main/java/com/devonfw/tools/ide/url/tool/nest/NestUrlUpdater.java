package com.devonfw.tools.ide.url.tool.nest;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for nest.
 */
public class NestUrlUpdater extends NpmBasedUrlUpdater {

  @Override
  public String getTool() {
    return "nest";
  }

  @Override
  protected String getPackageName() {
    return "@nestjs/cli";
  }

  @Override
  public String getCpeVendor() {
    return "nest";
  }

  @Override
  public String getCpeProduct() {
    return "nest";
  }
}
