package com.devonfw.tools.ide.url.tool.quarkus;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link QuarkusUrlUpdater} to allow integration testing with wiremock.
 */
public class QuarkusUrlUpdaterMock extends QuarkusUrlUpdater {

  private final String baseUrl;

  QuarkusUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getBaseUrl() {
    return this.baseUrl;
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";
  }
}

