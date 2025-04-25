package com.devonfw.tools.ide.url.tool.python;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * {@Link JsonUrlUpdater} test mock for Python
 */
public class PythonUrlUpdaterMock extends PythonUrlUpdater {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public PythonUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getVersionBaseUrl() {

    return wmRuntimeInfo.getHttpBaseUrl();
  }
}
