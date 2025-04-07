package com.devonfw.tools.ide.url.tool.gcviewer;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * {@link GithubUrlUpdater} for GCViewer.
 */
public class GcViewerUrlUpdater extends GithubUrlUpdater {

  private static final String BASE_URL = "https://sourceforge.net";

  @Override
  protected String getTool() {

    return "gcviewer";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getBaseUrl() + "/projects/gcviewer/files/gcviewer-${version}.jar");
  }

  @Override
  protected String getGithubOrganization() {

    return "chewiebug";
  }

  @Override
  protected String getGithubRepository() {

    return "GCViewer";
  }

  @Override
  protected String getBaseUrl() {

    return BASE_URL;
  }

  @Override
  protected String mapVersion(String version) {

    if (version.matches("\\d+\\.\\d+(\\.\\d+)?")) {
      return super.mapVersion(version);
    } else {
      return null;
    }
  }

  @Override
  public String getCpeVendor() {
    return "chewiebug";
  }

  @Override
  public String getCpeProduct() {
    return "gcviewer";
  }

}
