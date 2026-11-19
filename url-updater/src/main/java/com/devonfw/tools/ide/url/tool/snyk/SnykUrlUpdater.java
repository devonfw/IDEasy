package com.devonfw.tools.ide.url.tool.snyk;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for the Snyk CLI.
 * <p>
 * Follows the official releases of <a href="https://github.com/snyk/cli">github.com/snyk/cli</a>. Download URL pattern:
 * <a href="https://github.com/snyk/cli/releases/download/v1.1307.4/snyk-linux">github.com/snyk/cli/releases/download/v1.1307.4/snyk-linux</a>.
 * Assets are extensionless raw binaries (e.g. {@code snyk-linux}, {@code snyk-win.exe}).
 * <p>
 * Note: Snyk also publishes dedicated Alpine assets ({@code snyk-alpine}, {@code snyk-alpine-arm64}). Selecting
 * those would require Linux-distribution detection, which IDEasy does not have (its {@link OperatingSystem} model
 * only distinguishes Windows/macOS/Linux by OS and architecture). It is therefore intentionally out of scope for
 * this change: any Linux host is offered the generic glibc-based {@code snyk-linux}/{@code snyk-linux-arm64}
 * binaries, which are not guaranteed to run on musl-based Alpine. Revisit if distribution detection is added.
 */
public class SnykUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The constructor.
   */
  public SnykUrlUpdater() {

    super("https://github.com");
  }

  /**
   * Package-private constructor used for testing {@link SnykUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  SnykUrlUpdater(String baseUrl) {

    super(baseUrl, baseUrl);
  }

  @Override
  protected String getGithubOrganization() {

    return "snyk";
  }

  @Override
  public String getTool() {

    return "snyk";
  }

  @Override
  protected String getGithubRepository() {

    return "cli";
  }

  /**
   * Release tags are prefixed with {@code v} (e.g. {@code v1.1307.4}); the stored version strips it.
   */
  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    // Re-add the "v" in the release tag: ${version} substitutes to the stripped version (e.g. 1.1307.4).
    // Snyk asset names carry no version, so the suffix has no ${version}.
    String baseUrl = createGithubReleaseDownloadUrl("v${version}", "snyk-");

    doAddVersion(urlVersion, baseUrl + "linux", LINUX);
    doAddVersion(urlVersion, baseUrl + "linux-arm64", LINUX, SystemArchitecture.ARM64);
    doAddVersion(urlVersion, baseUrl + "macos", MAC);
    doAddVersion(urlVersion, baseUrl + "macos-arm64", MAC, SystemArchitecture.ARM64);
    doAddVersion(urlVersion, baseUrl + "win.exe", WINDOWS);
  }
}
