package com.devonfw.tools.ide.url.tool.az;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link AzureUrlUpdater} to allow integration testing with wiremock.
 */
public class AzureUrlUpdaterMock extends AzureUrlUpdater {

  private final String baseUrl;

  AzureUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
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

