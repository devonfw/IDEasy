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

  private static final String DOWNLOAD_BASE_URL = "https://azcliprod.blob.core.windows.net";
  private static final VersionIdentifier MIN_AZURE_VID = VersionIdentifier.of("2.17.0");

  private static final VersionIdentifier MIN_AZURE_MAC_VID = VersionIdentifier.of("2.84.0");

  /**
   * The Constructor.
   */
  public AzureUrlUpdater() {
    super(DOWNLOAD_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link AzureUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  AzureUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

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
      String macBaseUrl = getMacDownloadBaseUrl() + "/" + getGithubRepositoryPath()
          + "/releases/download/azure-cli-${version}/azure-cli-${version}-macos-";
      doAddVersion(urlVersion, macBaseUrl + "x86_64.tar.gz", OperatingSystem.MAC, SystemArchitecture.X64);
      doAddVersion(urlVersion, macBaseUrl + "arm64.tar.gz", OperatingSystem.MAC, SystemArchitecture.ARM64);
    }
  }

  /**
   * @return the base URL for the macOS tarball downloads. Windows MSIs live on Microsoft blob storage while mac tarballs are published as GitHub releases.
   */
  private String getMacDownloadBaseUrl() {

    String downloadBaseUrl = getDownloadBaseUrl();
    if (!DOWNLOAD_BASE_URL.equals(downloadBaseUrl)) {
      return downloadBaseUrl;
    }
    return GITHUB_BASE_URL;
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

  @Override
  protected void initCpe(CpeRegistry cpe) {
    cpe.addVendor("microsoft").addProduct("az").addProduct("azure_cli").addProduct("azure-command-line_interface");
  }
}
