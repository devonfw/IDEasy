package com.devonfw.tools.ide.url.tool.copilot;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link CopilotUrlUpdater} to allow integration testing with wiremock.
 */
public class CopilotUrlUpdaterMock extends CopilotUrlUpdater {

  private final String baseUrl;

  private final WireMockRuntimeInfo wmRuntimeInfo;

  CopilotUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
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
    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";
  }
}
