package com.devonfw.tools.ide.url.tool.mvnd;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for Maven Daemon (mvnd). Supports both stable releases (1.x) and pre-releases (2.x) to allow users to experiment with the
 * latest versions.
 */
public class MvndUrlUpdater extends GithubUrlReleaseUpdater {

  private static final VersionIdentifier MIN_MVND_VID = VersionIdentifier.of("1.0.2");

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
  protected boolean isVersionFiltered() {
    // Don't filter pre-releases, we'll handle filtering explicitly in mapVersion()
    return false;
  }

  @Override
  public String mapVersion(String version) {
    // Accept pre-release versions (rc, beta, alpha, etc.) first
    if (version.contains("-rc") || version.contains("-beta") || version.contains("-alpha")) {
      return version;
    }

    // For stable versions, require minimum version 1.0.2
    VersionIdentifier vid = VersionIdentifier.of(version);
    if (vid.isValid()) {
      VersionComparisonResult comparison = vid.compareVersion(MIN_MVND_VID);
      if (comparison.isGreater() || comparison.isEqual()) {
        return version;
      }
    }
    return null;
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
