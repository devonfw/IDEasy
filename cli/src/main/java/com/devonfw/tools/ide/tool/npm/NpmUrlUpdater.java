package com.devonfw.tools.ide.tool.npm;

import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for npm (node package manager).
 */
public class NpmUrlUpdater extends NpmBasedUrlUpdater {

  @Override
  protected String getTool() {

    return "npm";
  }

}