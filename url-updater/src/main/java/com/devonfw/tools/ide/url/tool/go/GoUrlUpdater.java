package com.devonfw.tools.ide.url.tool.go;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for Go programming language.
 */
public class GoUrlUpdater extends GithubUrlTagUpdater {

  private static final String GO_BASE_URL = "https://go.dev/dl/";

  private static final VersionIdentifier MIN_GO_VID = VersionIdentifier.of("go1.2.2");

  private static final VersionIdentifier MIN_WIN_ARM_VID = VersionIdentifier.of("go1.17");

  private static final VersionIdentifier MIN_MAC_ARM_VID = VersionIdentifier.of("go1.16");

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
    String baseUrl = getDownloadBaseUrl() + "${version}.";
    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    if (vid.compareVersion(MIN_GO_VID).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "windows-amd64.zip", WINDOWS, X64);
      if (vid.compareVersion(MIN_WIN_ARM_VID).isGreater()) {
        doAddVersion(urlVersion, baseUrl + "windows-arm64.zip", WINDOWS, ARM64);
      }
      doAddVersion(urlVersion, baseUrl + "linux-amd64.tar.gz", LINUX, X64);
      doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);
      doAddVersion(urlVersion, baseUrl + "darwin-amd64.tar.gz", MAC, X64);
      if (vid.compareVersion(MIN_MAC_ARM_VID).isGreater()) {
        doAddVersion(urlVersion, baseUrl + "darwin-arm64.tar.gz", MAC, ARM64);
      }
    }
  }

  @Override
  public String mapVersion(String version) {
    return super.mapVersion("go" + version);
  }
}

