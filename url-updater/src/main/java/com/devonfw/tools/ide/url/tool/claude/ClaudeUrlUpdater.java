package com.devonfw.tools.ide.url.tool.claude;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for Claude Code CLI.
 */
public class ClaudeUrlUpdater extends GithubUrlReleaseUpdater {

  private static final VersionIdentifier MIN_CLAUDE_VID = VersionIdentifier.of("2.1.117");

  @Override
  public String getTool() {
    return "claude";
  }

  @Override
  protected String getGithubOrganization() {
    return "anthropics";
  }

  @Override
  protected String getGithubRepository() {
    return "claude-code";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = createGithubReleaseDownloadUrl("v${version}", "claude-");
    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    VersionComparisonResult versionComparisonResult = vid.compareVersion(MIN_CLAUDE_VID);

    if (versionComparisonResult.isEqual() || versionComparisonResult.isGreater()) {

      doAddVersion(urlVersion, baseUrl + "linux-x64.tar.gz", LINUX, X64);
      doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);

      doAddVersion(urlVersion, baseUrl + "darwin-x64.tar.gz", MAC, X64);
      doAddVersion(urlVersion, baseUrl + "darwin-arm64.tar.gz", MAC, ARM64);

      doAddVersion(urlVersion, baseUrl + "win32-x64.zip", WINDOWS, X64);
      doAddVersion(urlVersion, baseUrl + "win32-arm64.zip", WINDOWS, ARM64);
    }
  }

  @Override
  protected String getVersionPrefixToRemove() {
    return "v";
  }

  @Override
  public String getCpeVendor() {
    return "claude-code";
  }

  @Override
  public String getCpeProduct() {
    return "claude-code";
  }
}
