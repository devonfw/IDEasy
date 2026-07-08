package com.devonfw.tools.ide.url.tool.sonar;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;

/**
 * {@link GithubUrlTagUpdater} for sonar (sonarqube).
 */
public class SonarUrlUpdater extends GithubUrlTagUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://binaries.sonarsource.com";

  /**
   * The Constructor.
   */
  public SonarUrlUpdater() {
    super(DOWNLOAD_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link SonarUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  SonarUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {

    return "sonar";
  }

  @Override
  protected String getGithubOrganization() {

    return "SonarSource";
  }

  @Override
  protected String getGithubRepository() {

    return "sonarqube";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getDownloadBaseUrl() + "/Distribution/sonarqube/sonarqube-${version}.zip");
  }

  @Override
  public String getCpeVendor() {
    return "sonarsource";
  }

  @Override
  public String getCpeProduct() {
    return "sonarqube";
  }
}
