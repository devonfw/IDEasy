package com.devonfw.tools.security;

import com.devonfw.tools.ide.url.tool.intellij.IntellijUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link IntellijUrlUpdater} to allow integration testing with wiremock.
 */
public class SecurityUrlUpdaterMock extends IntellijUrlUpdater {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public SecurityUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getVersionBaseUrl() {

    return wmRuntimeInfo.getHttpBaseUrl();
  }
}
