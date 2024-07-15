package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.github.GithubTag;
import com.devonfw.tools.ide.github.GithubTags;

/**
 * {@link JsonUrlUpdater} for github projects.
 */
public abstract class GithubUrlUpdater extends JsonUrlUpdater<GithubTags, GithubTag> {

  @Override
  protected String doGetVersionUrl() {

    return "https://api.github.com/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";

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
   * @return the github repository name (e.g. "cobigen").
   */
  protected String getGithubRepository() {

    return getTool();
  }

  @Override
  protected Collection<GithubTag> getVersionItems(GithubTags jsonObject) {

    return jsonObject;
  }

  @Override
  protected String getVersion(GithubTag jsonVersionItem) {

    return jsonVersionItem.getRef().replace("refs/tags/", "");
  }
}
