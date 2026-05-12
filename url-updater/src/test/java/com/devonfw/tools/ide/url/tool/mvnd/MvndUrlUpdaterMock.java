package com.devonfw.tools.ide.url.tool.mvnd;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link MvndUrlUpdater} to allow integration testing with wiremock.
 */
@SuppressWarnings("unused")
public class MvndUrlUpdaterMock extends MvndUrlUpdater {

  private final String baseUrl;

  MvndUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getVersionBaseUrl() {
    return this.baseUrl + "/repos/";
  }

  @Override
  protected String getDownloadBaseUrl() {
    return this.baseUrl + "/maven/mvnd/";
  }
}


