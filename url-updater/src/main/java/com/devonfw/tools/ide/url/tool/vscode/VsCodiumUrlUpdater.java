package com.devonfw.tools.ide.url.tool.vscode;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;

/**
 * {@link GithubUrlTagUpdater} for the "vscodium" edition of vscode (<a href="https://vscodium.com/">VSCodium</a>).
 */
public class VsCodiumUrlUpdater extends GithubUrlTagUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://github.com/VSCodium/vscodium/releases/download";

  @Override
  public String getTool() {

    return "vscode";
  }

  @Override
  protected String getEdition() {

    return "vscodium";
  }

  @Override
  protected String getGithubOrganization() {

    return "VSCodium";
  }

  @Override
  protected String getGithubRepository() {

    return "vscodium";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return DOWNLOAD_BASE_URL;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getDownloadBaseUrl() + "/${version}/VSCodium-";
    doAddVersion(urlVersion, baseUrl + "linux-x64-${version}.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "linux-arm64-${version}.tar.gz", LINUX, ARM64);
    doAddVersion(urlVersion, baseUrl + "darwin-x64-${version}.zip", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "darwin-arm64-${version}.zip", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "win32-x64-${version}.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "win32-arm64-${version}.zip", WINDOWS, ARM64);
  }

  @Override
  public String mapVersion(String version) {

    // VSCodium tag schemes seen in history: 3-segment "1.55.0", 4-segment "1.84.2.23319", and current 3-segment with
    // build-encoded patch like "1.116.02821". Accept any 3- or 4-segment numeric tag.
    if (version.matches("\\d+\\.\\d+\\.\\d+(\\.\\d+)?")) {
      return super.mapVersion(version);
    } else {
      return null;
    }
  }

  @Override
  public String getCpeVendor() {
    return "vscodium";
  }

  @Override
  public String getCpeProduct() {
    return "vscodium";
  }

}
