package com.devonfw.tools.ide.url.tool.vscode;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link VsCodiumUrlUpdater} to allow integration testing with wiremock.
 */
public class VsCodiumUrlUpdaterMock extends VsCodiumUrlUpdater {

  private final String baseUrl;

  VsCodiumUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
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
