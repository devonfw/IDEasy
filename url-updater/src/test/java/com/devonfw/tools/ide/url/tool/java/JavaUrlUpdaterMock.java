package com.devonfw.tools.ide.url.tool.java;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link JavaUrlUpdater} to allow integration testing with wiremock.
 */
public class JavaUrlUpdaterMock extends JavaUrlUpdater {

  private final String baseUrl;

  JavaUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {

    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getMirror() {

    return this.baseUrl + "/downloads/";
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/versions/";
  }
}
