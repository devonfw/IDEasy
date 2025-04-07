package com.devonfw.tools.ide.url.tool.quarkus;

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
  protected String getBaseUrl() {

    return GITHUB_BASE_URL;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    if (vid.compareVersion(MIN_QUARKUS_VID).isGreater()) {
      String baseUrl = getBaseUrl() + "/quarkusio/quarkus/releases/download/${version}/quarkus-cli-${version}";
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

  @Override
  public String mapUrlVersionToCpeVersion(String version) {

    return version.replaceAll("[^\\d.]", "");
  }
}
