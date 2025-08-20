package com.devonfw.tools.ide.url.tool.ng;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for angular.
 */
public class NgUrlUpdater extends NpmBasedUrlUpdater {

  @Override
  protected String getTool() {

    return "ng";
  }

  @Override
  protected String getPackageName() {

    return "@angular/cli";
  }

  @Override
  public String getCpeVendor() {
    return "npm";
  }

  @Override
  public String getCpeProduct() {
    return "npm";
  }
}
