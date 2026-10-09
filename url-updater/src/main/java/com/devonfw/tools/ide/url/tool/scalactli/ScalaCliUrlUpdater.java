package com.devonfw.tools.ide.url.tool.scalactli;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Scala CLI.
 * <p>
 * Scala CLI (https://scala-cli.virtuslab.org) is published as platform-specific binaries on GitHub Releases at
 * <a href="https://github.com/VirtusLab/scala-cli">VirtusLab/scala-cli</a>.
 * <p>
 * Download URL pattern: https://github.com/VirtusLab/scala-cli/releases/download/v${version}/scala-cli-${platform}.${ext}
 */
public class ScalaCliUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The constructor.
   */
  public ScalaCliUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link ScalaCliUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  ScalaCliUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {
    return "scala-cli";
  }

  @Override
  protected String getGithubOrganization() {
    return "VirtusLab";
  }

  @Override
  protected String getGithubRepository() {
    return "scala-cli";
  }

  @Override
  protected String getVersionPrefixToRemove() {
    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    // the release tag carries a "v" prefix (e.g. v1.17.1) while ${version} is the plain (mapped) version,
    // so the prefix has to be re-added when building the download URL
    String baseUrl = createGithubReleaseDownloadUrl("v${version}", "scala-cli-");

    doAddVersion(urlVersion, baseUrl + "x86_64-pc-linux.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "x86_64-apple-darwin.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "aarch64-apple-darwin.gz", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "x86_64-pc-win32.zip", WINDOWS, X64);
  }
}
