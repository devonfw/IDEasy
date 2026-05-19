package com.devonfw.tools.ide.url.tool.quarkus;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for quarkus CLI.
 */
public class QuarkusUrlUpdater extends GithubUrlTagUpdater {

  private static final VersionIdentifier MIN_QUARKUS_VID = VersionIdentifier.of("2.5.0");

  @Override
  public String getTool() {

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
      String baseUrl = createGithubReleaseDownloadUrl("${version}", "quarkus-cli-${version}");
      doAddVersion(urlVersion, baseUrl + ".zip", WINDOWS);
      doAddVersion(urlVersion, baseUrl + ".tar.gz");
    }
  }

  @Override
  public String getCpeVendor() {

    return "quarkus";
  }

  @Override
  public String getCpeProduct() {

    return "quarkus";
  }

}
