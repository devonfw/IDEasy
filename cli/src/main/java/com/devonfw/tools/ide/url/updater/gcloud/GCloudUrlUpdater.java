package com.devonfw.tools.ide.url.updater.gcloud;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for GCloud CLI.
 */
public class GCloudUrlUpdater extends GithubUrlUpdater {

  private static final VersionIdentifier MIN_GCLOUD_VID = VersionIdentifier.of("299.0.0");
  private static final VersionIdentifier MIN_ARM_GCLOUD_VID = VersionIdentifier.of("366.0.0");
  private static final String BASE_URL = "https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${version}-";
  
  @Override
  protected String getTool() {

    return "gcloud";
  }

  @Override
  protected String getGithubRepository() {

    return "google-cloud-sdk";
  }

  @Override
  protected String getGithubOrganization() {

    return "twistedpair";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    if (vid.compareVersion(MIN_GCLOUD_VID).isGreater()) {
      doAddVersion(urlVersion, BASE_URL + "windows-x86_64.zip", WINDOWS);
      doAddVersion(urlVersion, BASE_URL + "linux-x86_64.tar.gz", LINUX);
      doAddVersion(urlVersion, BASE_URL + "darwin-x86_64.tar.gz", MAC);
      if (vid.compareVersion(MIN_ARM_GCLOUD_VID).isGreater()) {
        doAddVersion(urlVersion, BASE_URL + "linux-arm.tar.gz", LINUX, ARM64);
        doAddVersion(urlVersion, BASE_URL + "darwin-arm.tar.gz", MAC, ARM64);
      }
    }
  }

}
