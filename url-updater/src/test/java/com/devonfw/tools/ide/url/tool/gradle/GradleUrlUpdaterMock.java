package com.devonfw.tools.ide.url.tool.gradle;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link GradleUrlUpdater} to allow integration testing with wiremock.
 */
public class GradleUrlUpdaterMock extends GradleUrlUpdater {

  private final String baseUrl;

  private final WireMockRuntimeInfo wmRuntimeInfo;

  GradleUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wireMockRuntimeInfo;
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getDownloadBaseUrl() {
    return this.baseUrl;
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/releases";
  }
}

