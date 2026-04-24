package com.devonfw.tools.ide.url.tool.nestjs;


import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link NestJsUrlUpdater} to allow integration testing with wiremock.
 */
public class NestJsUrlUpdaterMock extends NestJsUrlUpdater {

  private final String baseUrl;

  NestJsUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl;
  }

}
