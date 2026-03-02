package com.devonfw.tools.ide.url.tool.gcloud;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for GCloud CLI.
 */
public class GCloudUrlUpdater extends GithubUrlUpdater {

  private static final VersionIdentifier MIN_GCLOUD_VID = VersionIdentifier.of("299.0.0");
  private static final VersionIdentifier MIN_ARM_GCLOUD_VID = VersionIdentifier.of("366.0.0");

  @Override
  public String getTool() {

    return "gcloud";
  }

  @Override
  protected String getGithubRepository() {

    return "google-cloud-sdk";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://dl.google.com";
  }

  @Override
  protected String getGithubOrganization() {

    return "twistedpair";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    String baseUrl = getDownloadBaseUrl() + "/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${version}-";
    if (vid.compareVersion(MIN_GCLOUD_VID).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "windows-x86_64.zip", WINDOWS);
      doAddVersion(urlVersion, baseUrl + "linux-x86_64.tar.gz", LINUX);
      doAddVersion(urlVersion, baseUrl + "darwin-x86_64.tar.gz", MAC);
      if (vid.compareVersion(MIN_ARM_GCLOUD_VID).isGreater()) {
        doAddVersion(urlVersion, baseUrl + "linux-arm.tar.gz", LINUX, ARM64);
        doAddVersion(urlVersion, baseUrl + "darwin-arm.tar.gz", MAC, ARM64);
      }
    }
  }

  @Override
  public String getCpeVendor() {
    return "google";
  }

  @Override
  public String getCpeProduct() {
    return "gcloud";
  }

}
