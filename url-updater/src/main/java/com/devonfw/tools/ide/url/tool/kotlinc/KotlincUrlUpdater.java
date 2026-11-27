package com.devonfw.tools.ide.url.tool.kotlinc;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for kotlinc.
 */
public class KotlincUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The Constructor.
   */
  public KotlincUrlUpdater() {

    super();
  }

  /**
   * Package-private constructor used for testing {@link KotlincUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  KotlincUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {

    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  protected String getGithubOrganization() {

    return "JetBrains";
  }

  @Override
  protected String getGithubRepository() {

    return "kotlin";
  }

  @Override
  public String getTool() {

    return "kotlinc";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "Kotlin ";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getGithubReleaseDownloadBaseUrl() + "/v${version}/kotlin-compiler-${version}.zip");
  }

  @Override
  public String getCpeVendor() {
    return "jetbrains";
  }

  @Override
  public String getCpeProduct() {
    return "kotlin";
  }
}
