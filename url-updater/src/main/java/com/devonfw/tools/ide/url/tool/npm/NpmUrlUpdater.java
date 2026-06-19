package com.devonfw.tools.ide.url.tool.npm;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for npm (node package manager).
 */
public class NpmUrlUpdater extends NpmBasedUrlUpdater {

  /**
   * The Constructor.
   */
  public NpmUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link NpmUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  NpmUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {

    return "npm";
  }

  @Override
  protected String getPackageName() {

    return getTool();
  }

  @Override
  public String getCpeVendor() {
    return "npmjs";
  }

  @Override
  public String getCpeProduct() {
    return "npm";
  }
}
