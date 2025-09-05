package com.devonfw.tools.ide.url.tool.ng;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link NgUrlUpdater} to allow integration testing with wiremock.
 */
public class NgUrlUpdaterMock extends NgUrlUpdater {

  private final String baseUrl;

  NgUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl;
  }
}
