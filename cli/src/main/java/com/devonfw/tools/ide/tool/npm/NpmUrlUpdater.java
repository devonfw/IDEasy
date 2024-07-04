package com.devonfw.tools.ide.tool.npm;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;

/**
 * {@link NpmBasedUrlUpdater} for npm (node package manager).
 */
public class NpmUrlUpdater extends NpmBasedUrlUpdater {
  private static final String JSON_URL = "https://registry.npmjs.org/npm/";

  @Override
  protected String getTool() {

    return "npm";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, doGetVersionUrl() + "-/npm-${version}.tgz");
  }

}