package com.devonfw.tools.ide.url.tool.go;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;

/**
 * {@link GithubUrlTagUpdater} for Go programming language.
 */
public class GoUrlUpdater extends GithubUrlTagUpdater {

  private static final String GO_BASE_URL = "https://go.dev/dl/";

  @Override
  public String getTool() {
    return "go";
  }

  @Override
  protected String getGithubOrganization() {
    return "golang";
  }

  @Override
  protected String getGithubRepository() {
    return "go";
  }

  @Override
  protected String getVersionPrefixToRemove() {
    return "go";
  }

  @Override
  protected String getDownloadBaseUrl() {
    return GO_BASE_URL;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = getDownloadBaseUrl() + "go${version}.";
    doAddVersion(urlVersion, baseUrl + "windows-amd64.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "windows-arm64.zip", WINDOWS, ARM64);
    doAddVersion(urlVersion, baseUrl + "linux-amd64.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);
    doAddVersion(urlVersion, baseUrl + "darwin-amd64.tar.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "darwin-arm64.tar.gz", MAC, ARM64);
  }
}

