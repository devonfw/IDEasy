package com.devonfw.tools.ide.url.go;

import com.devonfw.tools.ide.url.tool.go.GoUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link GoUrlUpdater} to allow integration testing with wiremock.
 */
public class GoUrlUpdaterMock extends GoUrlUpdater {

  private final String baseUrl;

  public GoUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl;
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/repos/golang/go/git/refs/tags";
  }
}
