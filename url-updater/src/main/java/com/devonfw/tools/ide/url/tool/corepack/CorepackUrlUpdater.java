package com.devonfw.tools.ide.url.tool.corepack;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for Corepack.
 */
public class CorepackUrlUpdater extends NpmBasedUrlUpdater {

  @Override
  public String getTool() {

    return "corepack";
  }

  @Override
  protected String getPackageName() {

    return "corepack";
  }
  
}
