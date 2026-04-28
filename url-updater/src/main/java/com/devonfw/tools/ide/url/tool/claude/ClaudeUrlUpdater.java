package com.devonfw.tools.ide.url.tool.claude;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for GitHub Claude Code CLI.
 * <p>
 * Follows the official installation structure from GitHub's claude-code repository: https://github.com/anthropics/claude-code. Download URL pattern: <a
 * <p>
 * Download URL pattern: https://github.com/anthropics/claude-code/releases/download/v${VERSION}/claude-${PLATFORM}-${ARCH}.tar.gz
 */
public class ClaudeUrlUpdater extends GithubUrlTagUpdater {

  private static final VersionIdentifier MIN_CLAUDE_VID = VersionIdentifier.of("2.0.73");


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

    if (vid.compareVersion(MIN_CLAUDE_VID).isGreater()) {

      doAddVersion(urlVersion, baseUrl + "linux-x64.tar.gz", LINUX, X64);
      doAddVersion(urlVersion, baseUrl + "linux-x64-musl.tar.gz", LINUX, X64);
      doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);
      doAddVersion(urlVersion, baseUrl + "linux-arm64-musl.tar.gz", LINUX, ARM64);

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
}
