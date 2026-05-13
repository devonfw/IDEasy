package com.devonfw.tools.ide.url.tool.spyder;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link SpyderUrlUpdater} to allow integration testing with wiremock.
 */
public class SpyderUrlUpdaterMock extends SpyderUrlUpdater {

  private final String baseUrl;

  SpyderUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl;
  }

  @Override
  protected String doGetVersionUrl() {
    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";
  }

}
