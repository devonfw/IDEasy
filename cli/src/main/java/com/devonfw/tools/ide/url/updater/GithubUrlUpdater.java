package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.github.GithubTag;
import com.devonfw.tools.ide.github.GithubTags;

/**
 * {@link JsonUrlUpdater} for github projects.
 */
public abstract class GithubUrlUpdater extends JsonUrlUpdater<GithubTags> {
  @Override
  protected String doGetVersionUrl() {

    return "https://api.github.com/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";

  }

  @Override
  protected Class<GithubTags> getJsonObjectType() {

    return GithubTags.class;
  }

  @Override
  protected void collectVersionsFromJson(GithubTags jsonItem, Collection<String> versions) {

    for (GithubTag item : jsonItem) {
      String version = item.getRef().replace("refs/tags/", "");
      addVersion(version, versions);
    }
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

}
