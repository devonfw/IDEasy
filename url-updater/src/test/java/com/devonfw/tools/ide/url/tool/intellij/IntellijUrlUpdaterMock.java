package com.devonfw.tools.ide.url.tool.intellij;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link IntellijUrlUpdater} to allow integration testing with wiremock.
 */
public class IntellijUrlUpdaterMock extends IntellijUrlUpdater {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public IntellijUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getVersionBaseUrl() {

    return wmRuntimeInfo.getHttpBaseUrl();
  }
}
