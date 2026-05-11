package com.devonfw.tools.ide.url.tool.minikube;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;


/**
 * {@link GithubUrlReleaseUpdater} for Minikube.
 */
public class MinikubeUrlUpdater extends GithubUrlReleaseUpdater {

  private static final VersionIdentifier MIN_MINIKUBE_VID = VersionIdentifier.of("1.20.0");

  @Override
  public String getTool() {
    return "minikube";
  }

  @Override
  protected String getGithubOrganization() {
    return "kubernetes";
  }

  @Override
  protected String getGithubRepository() {
    return "minikube";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = createGithubReleaseDownloadUrl("v${version}", "minikube-");
    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    VersionComparisonResult versionComparisonResult = vid.compareVersion(MIN_MINIKUBE_VID);

    if (versionComparisonResult.isEqual() || versionComparisonResult.isGreater()) {
      doAddVersion(urlVersion, baseUrl + "linux-amd64.tar.gz", LINUX, X64);
      doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);

      doAddVersion(urlVersion, baseUrl + "darwin-amd64.tar.gz", MAC, X64);
      doAddVersion(urlVersion, baseUrl + "darwin-arm64.tar.gz", MAC, ARM64);

      doAddVersion(urlVersion, baseUrl + "windows-amd64.tar.gz", WINDOWS, X64);
    }
  }

  @Override
  protected String getVersionPrefixToRemove() {
    return "v";
  }

  @Override
  public String getCpeVendor() {
    return "minikube";
  }

  @Override
  public String getCpeProduct() {
    return "minikube";
  }
}

