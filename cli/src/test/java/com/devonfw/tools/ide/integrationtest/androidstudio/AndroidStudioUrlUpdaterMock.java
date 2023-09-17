package com.devonfw.tools.ide.integrationtest.androidstudio;

import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.devonfw.tools.ide.url.updater.androidstudio.AndroidStudioUrlUpdater;

/**
 * {@link JsonUrlUpdater} test mock for Android Studio.
 */
public class AndroidStudioUrlUpdaterMock extends AndroidStudioUrlUpdater {

  /** The base URL used for WireMock */
  private final static String TEST_BASE_URL = "http://localhost:8080";

  @Override
  protected String getVersionBaseUrl() {

    return TEST_BASE_URL;
  }
}
