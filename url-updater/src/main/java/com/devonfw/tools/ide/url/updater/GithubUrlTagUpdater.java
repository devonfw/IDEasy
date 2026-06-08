package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.github.GithubTag;
import com.devonfw.tools.ide.github.GithubTags;

/**
 * {@link AbstractGithubUrlUpdater} for GitHub projects.
 */
public abstract class GithubUrlTagUpdater extends AbstractGithubUrlUpdater<GithubTags, GithubTag> {

  public GithubUrlTagUpdater() {
    super();
  }

  public GithubUrlTagUpdater(String downloadBaseUrl) {
    super(downloadBaseUrl);
  }

  public GithubUrlTagUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + getGithubRepositoryPath() + "/git/refs/tags";

  }

  @Override
  protected Class<GithubTags> getJsonObjectType() {

    return GithubTags.class;
  }

  @Override
  protected Collection<GithubTag> getVersionItems(GithubTags jsonObject) {

    return jsonObject;
  }

}
