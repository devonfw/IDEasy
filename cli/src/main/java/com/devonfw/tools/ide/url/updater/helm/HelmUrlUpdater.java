package com.devonfw.tools.ide.url.updater.helm;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for "helm".
 */
public class HelmUrlUpdater extends GithubUrlUpdater {

  private static final VersionIdentifier MIN_MAC_ARM_VID = VersionIdentifier.of("3.4.0");

  @Override
  protected String getTool() {

    return "helm";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected String getGithubOrganization() {

    return "helm";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    String baseUrl = "https://get.helm.sh/helm-${version}-";
    doAddVersion(urlVersion, baseUrl + "windows-amd64.zip", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "linux-amd64.tar.gz", LINUX);
    doAddVersion(urlVersion, baseUrl + "darwin-amd64.tar.gz", MAC);
    if (vid.compareVersion(MIN_MAC_ARM_VID).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "darwin-arm64.tar.gz", MAC, ARM64);
    }
  }

  @Override
  protected String mapVersion(String version) {

    return super.mapVersion("v" + version);
  }


}
