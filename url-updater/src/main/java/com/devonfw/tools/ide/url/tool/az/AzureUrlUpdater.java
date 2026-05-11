package com.devonfw.tools.ide.url.tool.az;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for Azure-CLI.
 */
public class AzureUrlUpdater extends GithubUrlTagUpdater {

  private static final VersionIdentifier MIN_AZURE_VID = VersionIdentifier.of("2.17.0");

  private static final VersionIdentifier MIN_AZURE_MAC_VID = VersionIdentifier.of("2.84.0");

  @Override
  public String getTool() {

    return "az";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getDownloadBaseUrl() + "/msi/azure-cli-${version}.msi",
        OperatingSystem.WINDOWS);
    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    if (vid.compareVersion(MIN_AZURE_MAC_VID).isGreater()) {
      String macBaseUrl = GITHUB_BASE_URL + "/" + getGithubRepositoryPath()
          + "/releases/download/azure-cli-${version}/azure-cli-${version}-macos-";
      doAddVersion(urlVersion, macBaseUrl + "x86_64.tar.gz", OperatingSystem.MAC, SystemArchitecture.X64);
      doAddVersion(urlVersion, macBaseUrl + "arm64.tar.gz", OperatingSystem.MAC, SystemArchitecture.ARM64);
    }
  }

  @Override
  protected String getGithubOrganization() {

    return "Azure";
  }

  @Override
  protected String getGithubRepository() {

    return "azure-cli";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://azcliprod.blob.core.windows.net";
  }

  @Override
  public String mapVersion(String version) {

    version = version.substring(version.lastIndexOf("-") + 1);
    VersionIdentifier vid = VersionIdentifier.of(version);
    if (vid.isValid() && vid.compareVersion(MIN_AZURE_VID).isGreater()) {
      return super.mapVersion(version);
    } else {
      return null;
    }
  }

  @Override
  public String getCpeVendor() {
    return "microsoft";

  }

  @Override
  public String getCpeProduct() {
    return "az";
  }
}
