package com.devonfw.tools.ide.url.tool.pycharm;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

public class PycharmUrlUpdaterMock extends PycharmUrlUpdater {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public PycharmUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getVersionBaseUrl() {

    return wmRuntimeInfo.getHttpBaseUrl();
  }
}
