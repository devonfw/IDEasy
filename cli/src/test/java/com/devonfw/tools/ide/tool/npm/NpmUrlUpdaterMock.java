package com.devonfw.tools.ide.tool.npm;

/**
 * Mock of {@link NpmUrlUpdater} to allow integration testing with wiremock.
 */
public class NpmUrlUpdaterMock extends NpmUrlUpdater {
  private final static String TEST_URL = "http://localhost:8080/";

  @Override
  protected String getBaseUrl() {

    return TEST_URL;
  }
}
