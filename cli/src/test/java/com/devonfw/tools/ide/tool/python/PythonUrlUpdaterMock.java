package com.devonfw.tools.ide.tool.python;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * {@Link JsonUrlUpdater} test mock for Python
 */
public class PythonUrlUpdaterMock extends PythonUrlUpdater {

  WireMockRuntimeInfo wmRuntimeInfo;

  public PythonUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getVersionBaseUrl() {

    return wmRuntimeInfo.getHttpBaseUrl();
  }
}
