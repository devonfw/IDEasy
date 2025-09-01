package com.devonfw.tools.ide.url.tool.uv;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link UvUrlUpdater} to allow integration testing with wiremock.
 */
public class UvUrlUpdaterMock extends UvUrlUpdater {

  private final String baseUrl;

  public UvUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  public String getDownloadBaseUrl() {

    return this.baseUrl;
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";
  }
}
