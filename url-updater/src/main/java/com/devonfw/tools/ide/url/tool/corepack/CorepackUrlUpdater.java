package com.devonfw.tools.ide.url.tool.corepack;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for Corepack.
 */
public class CorepackUrlUpdater extends NpmBasedUrlUpdater {

  /**
   * The Constructor.
   */
  public CorepackUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link CorepackUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  CorepackUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {

    return "corepack";
  }

  @Override
  protected String getPackageName() {

    return "corepack";
  }

}
