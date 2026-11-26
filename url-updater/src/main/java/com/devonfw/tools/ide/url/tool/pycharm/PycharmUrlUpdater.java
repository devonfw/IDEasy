package com.devonfw.tools.ide.url.tool.pycharm;

import com.devonfw.tools.ide.url.updater.IdeaBasedUrlUpdater;

/**
 * {@link IdeaBasedUrlUpdater} base class for Pycharm.
 */
public class PycharmUrlUpdater extends IdeaBasedUrlUpdater {

  private static final String JSON_URL = "products?code=PCP%2CPCC&release.type=release";

  /**
   * The Constructor.
   */
  public PycharmUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link PycharmUrlUpdater}.
   *
   * @param versionBaseUrl mock url used as version base.
   */
  PycharmUrlUpdater(String versionBaseUrl) {
    super(versionBaseUrl);
  }

  @Override
  public String getTool() {

    return "pycharm";
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/" + JSON_URL;
  }

  @Override
  public String getCpeProduct() {
    return "pycharm";
  }
}
