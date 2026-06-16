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

    return jsonObject;
  }

}
