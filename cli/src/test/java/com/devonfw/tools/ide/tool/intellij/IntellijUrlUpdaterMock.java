package com.devonfw.tools.ide.tool.intellij;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link IntellijUrlUpdater} to allow integration testing with wiremock.
 */
public class IntellijUrlUpdaterMock extends IntellijUrlUpdater {

  WireMockRuntimeInfo wmRuntimeInfo;

  public IntellijUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getVersionBaseUrl() {

    return wmRuntimeInfo.getHttpBaseUrl();
  }
}
