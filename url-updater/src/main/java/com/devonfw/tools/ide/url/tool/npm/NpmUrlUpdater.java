package com.devonfw.tools.ide.url.tool.npm;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for npm (node package manager).
 */
public class NpmUrlUpdater extends NpmBasedUrlUpdater {

  @Override
  protected String getTool() {

    return "npm";
  }

  @Override
  protected String getPackageName() {

    return getTool();
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
