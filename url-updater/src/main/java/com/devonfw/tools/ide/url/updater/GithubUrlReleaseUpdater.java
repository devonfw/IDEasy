package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.github.GithubRelease;
import com.devonfw.tools.ide.github.GithubReleases;

/**
 * {@link JsonUrlUpdater} for GitHub projects.
 */
public abstract class GithubUrlReleaseUpdater extends JsonUrlUpdater<GithubReleases, GithubRelease> {

  /**
   * The default GitHub base url.
   */
  protected static final String GITHUB_BASE_URL = "https://github.com";

  @Override
  protected String getDownloadBaseUrl() {

    return GITHUB_BASE_URL;
  }

  @Override
  protected String getVersionBaseUrl() {

    return "https://api.github.com/repos/";
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + getGithubOrganization() + "/" + getGithubRepository() + "/releases";

  }

  @Override
  protected Class<GithubReleases> getJsonObjectType() {

    return GithubReleases.class;
  }

  /**
   * @return the github organization- or user-name (e.g. "devonfw").
   */
  protected abstract String getGithubOrganization();

  /**
   * @return the github repository name.
   */
  protected String getGithubRepository() {

    return getTool();
  }

  @Override
  protected Collection<GithubRelease> getVersionItems(GithubReleases jsonObject) {

    return jsonObject;
  }

}
