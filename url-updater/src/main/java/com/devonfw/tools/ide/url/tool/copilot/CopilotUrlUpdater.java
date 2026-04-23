package com.devonfw.tools.ide.url.tool.copilot;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for GitHub Copilot CLI.
 * <p>
 * Follows the official installation structure from GitHub's copilot-cli repository: https://github.com/github/copilot-cli/blob/main/install.sh
 * <p>
 * Download URL pattern: https://github.com/github/copilot-cli/releases/download/${VERSION}/copilot-${PLATFORM}-${ARCH}.tar.gz
 */
public class CopilotUrlUpdater extends GithubUrlTagUpdater {

  private static final String COPILOT_CLI_BASE_URL = "https://github.com";

  private static final VersionIdentifier MIN_COPILOT_VID = VersionIdentifier.of("1.0.2");

  @Override
  public String getTool() {
    return "copilot";
  }

  @Override
  protected String getGithubOrganization() {
    return "github";
  }

  @Override
  protected String getGithubRepository() {
    return "copilot-cli";
  }

  @Override
  protected String getDownloadBaseUrl() {
    return COPILOT_CLI_BASE_URL;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = getDownloadBaseUrl() + "/github/copilot-cli/releases/download/v${version}/copilot-";
    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    if (vid.compareVersion(MIN_COPILOT_VID).isGreater()) {

      doAddVersion(urlVersion, baseUrl + "linux-x64.tar.gz", LINUX, X64);
      doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);

      doAddVersion(urlVersion, baseUrl + "darwin-x64.tar.gz", MAC, X64);
      doAddVersion(urlVersion, baseUrl + "darwin-arm64.tar.gz", MAC, ARM64);

      doAddVersion(urlVersion, baseUrl + "x64.msi", WINDOWS, X64);
      doAddVersion(urlVersion, baseUrl + "arm64.msi", WINDOWS, ARM64);
    }
  }

  @Override
  protected String getVersionPrefixToRemove() {
    return "v";
  }

}
