package com.devonfw.tools.ide.url.tool.androidstudio;

import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * {@link JsonUrlUpdater} test mock for Android Studio.
 */
public class AndroidStudioUrlUpdaterMock extends AndroidStudioUrlUpdater {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public AndroidStudioUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getVersionBaseUrl() {

    return wmRuntimeInfo.getHttpBaseUrl();
  }
}
