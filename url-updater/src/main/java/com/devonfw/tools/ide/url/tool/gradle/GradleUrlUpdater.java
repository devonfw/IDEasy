package com.devonfw.tools.ide.url.tool.gradle;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Gradle.
 */
public class GradleUrlUpdater extends GithubUrlReleaseUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://services.gradle.org";

  /**
   * The Constructor.
   */
  public GradleUrlUpdater() {
    super(DOWNLOAD_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link GradleUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  GradleUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  protected String getGithubOrganization() {
    return "gradle";
  }

  @Override
  protected String getGithubRepository() {

    return "gradle";
  }

  @Override
  public String getTool() {

    return "gradle";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String downloadUrl = getDownloadBaseUrl() + "/distributions/gradle-" + mapVersion(urlVersion.getName()) + "-bin.zip";
    doAddVersion(urlVersion, downloadUrl);

  }

  @Override
  public String mapVersion(String version) {
    version = version.replace(" ", "-");
    return super.mapVersion(version);
  }

  @Override
  public String getCpeVendor() {
    return "gradle";
  }

  @Override
  public String getCpeProduct() {
    return "gradle";
  }
}
