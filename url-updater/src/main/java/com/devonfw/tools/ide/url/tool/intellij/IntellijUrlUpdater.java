package com.devonfw.tools.ide.url.tool.intellij;

import com.devonfw.tools.ide.url.updater.IdeaBasedUrlUpdater;

/**
 * {@link IdeaBasedUrlUpdater} base class for IntelliJ.
 */
public class IntellijUrlUpdater extends IdeaBasedUrlUpdater {

  private static final String JSON_URL = "products?code=IIU%2CIIC&release.type=release";

  /**
   * The Constructor.
   */
  public IntellijUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link IntellijUrlUpdater}.
   *
   * @param versionBaseUrl mock url used as version base.
   */
  IntellijUrlUpdater(String versionBaseUrl) {
    super(versionBaseUrl);
  }

  @Override
  public String getTool() {

    return "intellij";
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/" + JSON_URL;
  }

  @Override
  public String getCpeProduct() {
    return "intellij_idea";
  }


}
