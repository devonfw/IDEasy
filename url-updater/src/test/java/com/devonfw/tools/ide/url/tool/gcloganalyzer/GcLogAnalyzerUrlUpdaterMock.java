package com.devonfw.tools.ide.url.tool.gcloganalyzer;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link GcLogAnalyzerUrlUpdater} to allow integration testing with wiremock.
 */

public class GcLogAnalyzerUrlUpdaterMock extends GcLogAnalyzerUrlUpdater {

  private final String baseUrl;

  /**
   * The constructor.
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo} holding the WireMock base URL.
   */
  GcLogAnalyzerUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getVersionBaseUrl() {

    return this.baseUrl;
  }

  @Override
  protected String getDownloadBaseUrl() {
    return this.baseUrl;
  }

}
