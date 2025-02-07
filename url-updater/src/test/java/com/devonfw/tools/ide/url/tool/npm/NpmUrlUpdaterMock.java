package com.devonfw.tools.ide.url.tool.npm;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link NpmUrlUpdater} to allow integration testing with wiremock.
 */
public class NpmUrlUpdaterMock extends NpmUrlUpdater {

  private final String baseUrl;

  NpmUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/";
  }

  @Override
  protected String getBaseUrl() {

    return this.baseUrl;
  }
}
