package com.devonfw.tools.ide.url.tool.gradle;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Gradle.
 */
public class GradleUrlUpdater extends GithubUrlReleaseUpdater {


  @Override
  public String getGithubOrganization() {
    return "gradle";
  }

  @Override
  public String getGithubRepository() {

    return "gradle";
  }

  @Override
  public String getDownloadBaseUrl() {

    return "https://services.gradle.org";
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
