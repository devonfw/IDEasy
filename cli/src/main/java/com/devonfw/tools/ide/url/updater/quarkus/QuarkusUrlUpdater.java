package com.devonfw.tools.ide.url.updater.quarkus;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for quarkus CLI.
 */
public class QuarkusUrlUpdater extends GithubUrlUpdater {

  private static final VersionIdentifier MIN_QUARKUS_VID = VersionIdentifier.of("2.5.0");

  @Override
  protected String getTool() {

    return "quarkus";
  }

  @Override
  protected String getGithubOrganization() {

    return "quarkusio";
  }

  @Override
  protected String getGithubRepository() {

    return "quarkus";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    if (vid.compareVersion(MIN_QUARKUS_VID).isGreater()) {
      String baseUrl = "https://github.com/quarkusio/quarkus/releases/download/${version}/quarkus-cli-${version}";
      doAddVersion(urlVersion, baseUrl + ".zip", WINDOWS);
      doAddVersion(urlVersion, baseUrl + ".tar.gz");
    }
  }

}
