package com.devonfw.tools.ide.url.tool.inso;


import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/** Mock of {@link InsoUrlUpdater} to allow integration testing with WireMock.
    */
public class InsoUrlUpdaterMock extends InsoUrlUpdater {

  private final String baseUrl;

  private final WireMockRuntimeInfo wmRuntimeInfo;

  InsoUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
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

