package com.devonfw.tools.ide.url.tool.agy;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Anti-Gravity
 */
public class AgyUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The Constructor
   */
  public AgyUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link AgyUrlUpdater}
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  AgyUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {
    return "agy";
  }

  @Override
  protected String getGithubOrganization() {
    return "google-antigravity";
  }

  @Override
  protected String getGithubRepository() {
    return "antigravity-cli";
  }


  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = createGithubReleaseDownloadUrl("${version}", "");

    doAddVersion(urlVersion, baseUrl + "agy_cli_windows_x64.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "agy_cli_windows_arm64.zip", WINDOWS, ARM64);

    doAddVersion(urlVersion, baseUrl + "agy_cli_mac_x64.tar.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "agy_cli_mac_arm64.tar.gz", MAC, ARM64);

    doAddVersion(urlVersion, baseUrl + "agy_cli_linux_x64.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "agy_cli_linux_arm64.tar.gz", LINUX, ARM64);
  }

  @Override
  public String getCpeVendor() {
    return "google";
  }

  @Override
  public String getCpeProduct() {
    return "antigravity-cli";
  }

}
