package com.devonfw.tools.ide.url.tool.nest;


import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link NestUrlUpdater} to allow integration testing with wiremock.
 */
public class NestUrlUpdaterMock extends NestUrlUpdater {

  private final String baseUrl;

  NestUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl;
  }

}
