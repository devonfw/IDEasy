package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.github.GithubRelease;
import com.devonfw.tools.ide.github.GithubReleases;

/**
 * {@link AbstractGithubUrlUpdater} for GitHub projects.
 */
public abstract class GithubUrlReleaseUpdater extends AbstractGithubUrlUpdater<GithubReleases, GithubRelease> {

  public GithubUrlReleaseUpdater() {
    super();
  }

  public GithubUrlReleaseUpdater(String downloadBaseUrl) {
    super(downloadBaseUrl);
  }

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
