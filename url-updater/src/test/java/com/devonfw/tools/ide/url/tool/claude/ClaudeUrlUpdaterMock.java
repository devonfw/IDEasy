package com.devonfw.tools.ide.url.tool.claude;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link ClaudeUrlUpdater} to allow integration testing with wiremock.
 */
public class ClaudeUrlUpdaterMock extends ClaudeUrlUpdater {

  private final String baseUrl;

  private final WireMockRuntimeInfo wmRuntimeInfo;

  ClaudeUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
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


