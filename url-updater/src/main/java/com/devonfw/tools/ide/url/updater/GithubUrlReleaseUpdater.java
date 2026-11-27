package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.github.GithubRelease;
import com.devonfw.tools.ide.github.GithubReleases;

/**
 * {@link AbstractGithubUrlUpdater} for GitHub projects.
 */
public abstract class GithubUrlReleaseUpdater extends AbstractGithubUrlUpdater<GithubReleases, GithubRelease> {

  /**
   * Constructor with defaults for download- and version base url.
   */
  public GithubUrlReleaseUpdater() {
    super();
  }

  /**
   * Constructor with specific download base url.
   *
   * @param downloadBaseUrl url used as download base.
   */
  public GithubUrlReleaseUpdater(String downloadBaseUrl) {
    super(downloadBaseUrl);
  }

  /**
   * Constructor used within tests to override production defaults for download und version base url.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  public GithubUrlReleaseUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  protected String doGetVersionUrl() {

    return getGithubReleasesApiUrl();

  }

  @Override
  protected Class<GithubReleases> getJsonObjectType() {

    return GithubReleases.class;
  }

  @Override
  protected Collection<GithubRelease> getVersionItems(GithubReleases jsonObject) {

    if (acceptPreReleases()) {
      return jsonObject;
    }
    // Filter out releases GitHub flags as pre-release. This is not caught by the suffix-based AbstractUrlUpdater#isAcceptVersion, because a release can be
    // marked pre-release while its version string still looks stable (e.g. Ollama v0.40.0-rc3 tagged v0.40.0), so it would otherwise be published as stable.
    return jsonObject.stream().filter(release -> !release.prerelease()).toList();
  }

  /**
   * @return {@code true} to keep releases that GitHub marks as {@code prerelease}, {@code false} otherwise (default). Only tools that genuinely publish
   *     RC/beta releases (e.g. Maven Daemon, RubyInstaller) should override this to {@code true}.
   */
  protected boolean acceptPreReleases() {

    return false;
  }

}
