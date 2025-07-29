package com.devonfw.tools.ide.url.tool.sonar;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link SonarUrlUpdater} to allow integration testing with wiremock.
 */
public class SonarUrlUpdaterMock extends SonarUrlUpdater {

  private final String baseUrl;

  public SonarUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  public String getBaseUrl() {

    return this.baseUrl;
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";
  }
}

