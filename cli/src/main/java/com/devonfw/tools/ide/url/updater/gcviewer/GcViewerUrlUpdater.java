package com.devonfw.tools.ide.url.updater.gcviewer;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * {@link GithubUrlUpdater} for GCViewer.
 */
public class GcViewerUrlUpdater extends GithubUrlUpdater {
  @Override
  protected String getTool() {

    return "gcviewer";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, "https://sourceforge.net/projects/gcviewer/files/gcviewer-${version}.jar");
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
  protected String mapVersion(String version) {

    if (version.matches("\\d+\\.\\d+(\\.\\d+)?")) {
      return super.mapVersion(version);
    } else {
      return null;
    }
  }
}
