package com.devonfw.tools.ide.url.tool.helm;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for "helm".
 */
public class HelmUrlUpdater extends GithubUrlUpdater {

  private static final VersionIdentifier MIN_MAC_ARM_VID = VersionIdentifier.of("3.4.0");
  private static final String BASE_URL = "https://get.helm.sh";

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
  protected String getBaseUrl() {

    return BASE_URL;
  }

  @Override
  public String getCpeVendor() {

    return "helm";
  }

  @Override
  public String getCpeProduct() {

    return "helm";
  }

  @Override
  public String mapUrlVersionToCpeVersion(String version) {

    return version.substring(getVersionPrefixToRemove().length());
  }

  @Override
  public String mapCpeVersionToUrlVersion(String version) {

    return getVersionPrefixToRemove() + version;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    String baseUrl = getBaseUrl() + "/helm-${version}-";
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
