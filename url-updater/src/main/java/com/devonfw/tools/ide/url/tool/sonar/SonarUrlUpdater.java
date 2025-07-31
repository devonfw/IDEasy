package com.devonfw.tools.ide.url.tool.sonar;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * {@link GithubUrlUpdater} for sonar (sonarqube).
 */
public class SonarUrlUpdater extends GithubUrlUpdater {

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
  protected String getDownloadBaseUrl() {

    return "https://binaries.sonarsource.com";
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
    return "sonar";
  }
}
