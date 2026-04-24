package com.devonfw.tools.ide.url.tool.nestjs;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for nestjs.
 */
public class NestJsUrlUpdater extends NpmBasedUrlUpdater {

  @Override
  public String getTool() {
    return "nestjs";
  }

  @Override
  protected String getPackageName() {
    return "@nestjs/cli";
  }

  @Override
  public String getCpeVendor() {
    return "nestjs";
  }

  @Override
  public String getCpeProduct() {
    return "nestjs";
  }
}
