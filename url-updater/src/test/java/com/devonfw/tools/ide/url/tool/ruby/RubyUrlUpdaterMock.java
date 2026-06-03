package com.devonfw.tools.ide.url.tool.ruby;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link RubyUrlUpdater} to allow integration testing with WireMock.
 */
public class RubyUrlUpdaterMock extends RubyUrlUpdater {

  private final String baseUrl;

  private final WireMockRuntimeInfo wmRuntimeInfo;

  RubyUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
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
