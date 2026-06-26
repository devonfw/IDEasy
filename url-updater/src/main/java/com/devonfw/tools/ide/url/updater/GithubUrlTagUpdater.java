package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.github.GithubTag;
import com.devonfw.tools.ide.github.GithubTags;

/**
 * {@link AbstractGithubUrlUpdater} for GitHub projects.
 */
public abstract class GithubUrlTagUpdater extends AbstractGithubUrlUpdater<GithubTags, GithubTag> {

  /**
   * The Constructor.
   */
  public GithubUrlTagUpdater() {
    super();
  }

  /**
   * Constructor with external download base url.
   *
   * @param downloadBaseUrl url used for download base.
   */
  public GithubUrlTagUpdater(String downloadBaseUrl) {
    super(downloadBaseUrl);
  }

  /**
   * Constructor used within tests to override production defaults for download und version base url.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
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
