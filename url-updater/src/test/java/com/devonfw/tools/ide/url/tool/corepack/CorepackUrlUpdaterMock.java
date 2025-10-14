package com.devonfw.tools.ide.url.tool.corepack;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link CorepackUrlUpdater} to allow integration testing with wiremock.
 */
public class CorepackUrlUpdaterMock extends CorepackUrlUpdater {

  private final String baseUrl;

  CorepackUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl;
  }
}
