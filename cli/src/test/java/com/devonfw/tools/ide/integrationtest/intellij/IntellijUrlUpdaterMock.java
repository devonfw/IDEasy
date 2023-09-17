package com.devonfw.tools.ide.integrationtest.intellij;

import com.devonfw.tools.ide.url.updater.intellij.IntellijUrlUpdater;

/**
 * Mock of {@link IntellijUrlUpdater} to allow integration testing with wiremock.
 */
public class IntellijUrlUpdaterMock extends IntellijUrlUpdater {
  private final static String TEST_BASE_URL = "http://localhost:8080";

  @Override
  protected String getVersionBaseUrl() {

    return TEST_BASE_URL;
  }
}
