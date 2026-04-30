package com.devonfw.tools.ide.url.tool.cdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link CdkUrlUpdater} to allow integration testing with wiremock.
 */
public class CdkUrlUpdaterMock extends CdkUrlUpdater {

  private final String baseUrl;

  CdkUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl;
  }
}
