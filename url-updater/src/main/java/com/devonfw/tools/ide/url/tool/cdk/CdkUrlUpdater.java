package com.devonfw.tools.ide.url.tool.cdk;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for nest.
 */
public class CdkUrlUpdater extends NpmBasedUrlUpdater {

  @Override
  public String getTool() {
    return "cdk";
  }

  @Override
  protected String getPackageName() {
    return "aws-cdk";
  }

  @Override
  public String getCpeVendor() {
    return "cdk";
  }

  @Override
  public String getCpeProduct() {
    return "cdk";
  }
}
