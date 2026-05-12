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

  /**
   * @return URL path segment for the configured GitHub repository ({@code <org>/<repo>}).
   */
  protected String getGithubRepositoryPath() {

    return getGithubOrganization() + "/" + getGithubRepository();
  }

  /**
   * @return GitHub API URL of the releases endpoint for the configured repository.
   */
  protected String getGithubReleasesApiUrl() {

    return getVersionBaseUrl() + getGithubRepositoryPath() + "/releases";
  }

  /**
   * @return GitHub releases download base URL following GitHub conventions.
   */
  protected String getGithubReleaseDownloadBaseUrl() {

    return getDownloadBaseUrl() + "/" + getGithubRepositoryPath() + "/releases/download";
  }

  /**
   * Creates a release asset download URL for a given release tag and asset name.
   *
   * @param releaseTag release tag segment used in the URL path.
   * @param assetName release asset file name.
   * @return full URL to the release asset.
   */
  protected String createGithubReleaseDownloadUrl(String releaseTag, String assetName) {

    return getGithubReleaseDownloadBaseUrl() + "/" + releaseTag + "/" + assetName;
  }
}
