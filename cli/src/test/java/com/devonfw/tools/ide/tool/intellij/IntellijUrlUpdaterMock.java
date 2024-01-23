package com.devonfw.tools.ide.tool.intellij;

/**
 * Mock of {@link IntellijUrlUpdater} to allow integration testing with wiremock.
 */
public class IntellijUrlUpdaterMock extends IntellijUrlUpdater {
  private final static String TEST_BASE_URL = "http://localhost:8080";

  @Override
  protected String getVersionBaseUrl() {

    return TEST_BASE_URL;
  }

  @Override
  protected String getEdition() {

    return null;
  }

  @Override
  IntellijJsonObject getIntellijJsonRelease(IntellijJsonObject[] releases) {

    return null;
  }
}
