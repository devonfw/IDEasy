package com.devonfw.tools.ide.url.tool.mvnd;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Maven Daemon (mvnd). Supports both stable releases (1.x) and pre-releases (2.x) to allow users to experiment with the
 * latest versions.
 */
public class MvndUrlUpdater extends GithubUrlReleaseUpdater {

  @Override
  public String getTool() {

    return "mvnd";
  }

  @Override
  protected String getGithubOrganization() {

    return "apache";
  }

  @Override
  protected String getGithubRepository() {

    return "maven-mvnd";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://dlcdn.apache.org/maven/mvnd/";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    // Support both regular releases and pre-releases (e.g., 2.x versions)
    // This allows users to test the latest versions before they become stable
    String baseUrl = getDownloadBaseUrl() + "${version}/maven-mvnd-${version}-";

    // Windows
    doAddVersion(urlVersion, baseUrl + "windows-amd64.zip", WINDOWS, X64);

    // macOS
    doAddVersion(urlVersion, baseUrl + "darwin-amd64.zip", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "darwin-aarch64.zip", MAC, ARM64);

    // Linux
    doAddVersion(urlVersion, baseUrl + "linux-amd64.zip", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "linux-aarch64.zip", LINUX, ARM64);
  }
}
