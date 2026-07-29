package com.devonfw.tools.ide.url.tool.obsidian;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Obsidian.
 */
public class ObsidianUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The Constructor.
   */
  public ObsidianUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link ObsidianUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  ObsidianUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {
    return "obsidian";
  }

  @Override
  protected String getGithubOrganization() {
    return "obsidianmd";
  }

  @Override
  protected String getGithubRepository() {
    return "obsidian-releases";
  }

  @Override
  protected String getVersionPrefixToRemove() {
    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = createGithubReleaseDownloadUrl("v${version}", "");

    doAddVersion(urlVersion, baseUrl + "Obsidian-${version}.exe", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "Obsidian-${version}.dmg", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "obsidian_${version}_amd64.deb", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "obsidian-${version}.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "obsidian-${version}-arm64.tar.gz", LINUX, ARM64);
  }

  @Override
  public String getCpeVendor() {
    return "obsidian";
  }

  @Override
  public String getCpeProduct() {
    return "obsidian";
  }
}
