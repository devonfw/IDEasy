package com.devonfw.tools.ide.url.tool.pnpm;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for pnpm.
 * <p>
 * pnpm (https://pnpm.io) is a fast, disk-space-efficient package manager for JavaScript and Node.js that is distributed as a self-contained standalone
 * executable. It is published as git tags (e.g. {@code v12.12.1}) on GitHub at <a href="https://github.com/pnpm/pnpm">pnpm/pnpm</a>, with the per-platform
 * release assets attached to the corresponding release.
 * <p>
 * Download URL pattern: https://github.com/pnpm/pnpm/releases/download/v${version}/pnpm-${platform}.${ext}
 */
public class PnpmUrlUpdater extends GithubUrlTagUpdater {

  /** The minimum version with the current (post v11) release asset naming; older releases used a different asset naming scheme. */
  public static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("11.0.0");

  /**
   * The constructor.
   */
  public PnpmUrlUpdater() {

    super();
  }

  /**
   * Package-private constructor used for testing {@link PnpmUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  PnpmUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {

    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {

    return "pnpm";
  }

  @Override
  protected String getGithubOrganization() {

    return "pnpm";
  }

  @Override
  protected String getGithubRepository() {

    return "pnpm";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected boolean isAcceptVersion(String version) {

    VersionIdentifier vid = VersionIdentifier.of(version);
    return (vid != null) && vid.isGreaterOrEqual(MIN_VERSION) && super.isAcceptVersion(version);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    // the git tag carries a "v" prefix (v${version}) while ${version} is the plain (mapped) version, so the prefix is re-added when building the download URL
    String baseUrl = createGithubReleaseDownloadUrl("v${version}", "pnpm-");
    doAddVersion(urlVersion, baseUrl + "linux-x64.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);
    doAddVersion(urlVersion, baseUrl + "darwin-x64.tar.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "darwin-arm64.tar.gz", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "win32-x64.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "win32-arm64.zip", WINDOWS, ARM64);
  }
}
