package com.devonfw.tools.ide.url.tool.vscode;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link VsCodeUrlUpdater} to allow integration testing with wiremock.
 */
public class VsCodeUrlUpdaterMock extends VsCodeUrlUpdater {

  private final String baseUrl;

  VsCodeUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
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

