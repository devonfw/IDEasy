package com.devonfw.tools.ide.tool.npm;

/**
 * Mock of {@link NpmUrlUpdater} to allow integration testing with wiremock.
 */
public class NpmUrlUpdaterMock extends NpmUrlUpdater {
  private final static String TEST_URL = "http://localhost:8080/npm/";

  @Override
  protected String doGetVersionUrl() {

    return TEST_URL;
  }
}
