package com.devonfw.tools.ide.url.tool.sonar;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * {@link GithubUrlUpdater} for sonar (sonarqube).
 */
public class SonarUrlUpdater extends GithubUrlUpdater {

  private static final String BASE_URL = "https://binaries.sonarsource.com";

  @Override
  protected String getTool() {

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
  protected String getBaseUrl() {

    return BASE_URL;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getBaseUrl() + "/Distribution/sonarqube/sonarqube-${version}.zip");
  }

  @Override
  public String getCpeVendor() {
    return "sonarsource";
  }

  @Override
  public String getCpeProduct() {
    return "sonar";
  }
}
