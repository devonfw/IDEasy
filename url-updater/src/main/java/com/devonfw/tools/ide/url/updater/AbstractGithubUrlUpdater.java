package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.json.JsonVersionItem;

/**
 * {@link JsonUrlUpdater} for GitHub projects.
 */
public abstract class AbstractGithubUrlUpdater<J extends JsonObject, JVI extends JsonVersionItem> extends JsonUrlUpdater<J, JVI> {

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
}
