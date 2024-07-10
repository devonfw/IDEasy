package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.github.GithubTag;
import com.devonfw.tools.ide.github.GithubTags;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;

import java.util.Collection;

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

  @Override
  protected void collectVersionsFromJson(GithubTags jsonItem, Collection<String> versions) {

    for (GithubTag item : jsonItem) {
      String version = item.getRef().replace("refs/tags/", "");
      version = mapVersion(version);
      addVersion(version, versions);
    }
  }

  @Override
  protected void collectVersionsWithDownloadsFromJson(GithubTags jsonItem, UrlEdition edition) {

    throw new IllegalStateException();
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
    //TODO
    throw new IllegalStateException();
  }

  @Override
  protected String getDownloadUrl(GithubTag jsonVersionItem) {
    //TODO
    throw new IllegalStateException();
  }

  @Override
  protected String getVersion(GithubTag jsonVersionItem) {
    //TODO
    throw new IllegalStateException();
  }
}
