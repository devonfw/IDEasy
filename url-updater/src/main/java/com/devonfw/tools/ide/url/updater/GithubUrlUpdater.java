package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.github.GithubTag;
import com.devonfw.tools.ide.github.GithubTags;

/**
 * {@link JsonUrlUpdater} for GitHub projects.
 */
public abstract class GithubUrlUpdater extends JsonUrlUpdater<GithubTags, GithubTag> {

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

    return getVersionBaseUrl() + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";

  }

  @Override
  protected Class<GithubTags> getJsonObjectType() {

    return GithubTags.class;
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
  protected Collection<GithubTag> getVersionItems(GithubTags jsonObject) {

    return jsonObject;
  }

}
