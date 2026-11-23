package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * {@link JsonUrlUpdater} for GitHub projects.
 */
public abstract class AbstractGithubUrlUpdater<J extends JsonObject, JVI extends JsonVersionItem> extends JsonUrlUpdater<J, JVI> {

  private static final ObjectMapper MAPPER = JsonMapping.createWithReflectionSupportForUrlUpdaters();

  /**
   * The default GitHub base url.
   */
  protected static final String GITHUB_BASE_URL = "https://github.com";

  protected static final String GITHUB_VERSION_URL = "https://api.github.com/repos/";

  /**
   * Number of results requested per GitHub API page.
   */
  protected static final int GITHUB_RESULTS_PER_PAGE = 50;

  /**
   * Constructor with defaults for download- and version base url.
   */
  public AbstractGithubUrlUpdater() {
    super(GITHUB_BASE_URL, GITHUB_VERSION_URL);
  }

  /**
   * Constructor with specific download base url.
   *
   * @param downloadBaseUrl url used as download base.
   */
  public AbstractGithubUrlUpdater(String downloadBaseUrl) {
    super(downloadBaseUrl, GITHUB_VERSION_URL);
  }

  /**
   * Constructor used within tests to override production defaults for download und version base url. To prevent repetition, "/repos/" will be added here to
   * mock version base url.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  public AbstractGithubUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl + "/repos/");
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

  @Override
  protected String doGetVersionResponseBodyAsString(String url) {

    ArrayNode combinedResponse = MAPPER.createArrayNode();
    int page = 1;
    int resultCount;

    do {
      String pageUrl = getGithubPageUrl(url, page);
      String response = super.doGetVersionResponseBodyAsString(pageUrl);

      try {
        JsonNode pageResponse = MAPPER.readTree(response);
        if (!pageResponse.isArray()) {
          throw new IllegalStateException("Expected GitHub API response to be a JSON array: " + pageUrl);
        }

        ArrayNode pageItems = (ArrayNode) pageResponse;
        resultCount = pageItems.size();
        combinedResponse.addAll(pageItems);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to process GitHub API response from: " + pageUrl, e);
      }

      page++;
    } while (resultCount == GITHUB_RESULTS_PER_PAGE);

    return combinedResponse.toString();
  }

  /**
   * Adds GitHub pagination parameters to the given URL.
   *
   * @param url the GitHub API URL.
   * @param page the page number.
   * @return the URL including pagination parameters.
   */
  protected String getGithubPageUrl(String url, int page) {

    String separator = url.contains("?") ? "&" : "?";
    return url + separator + "per_page=" + GITHUB_RESULTS_PER_PAGE + "&page=" + page;
  }
}
