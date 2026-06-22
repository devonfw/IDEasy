package com.devonfw.tools.ide.url.tool.java;

/**
 * Mock of {@link JavaUrlUpdater} to allow integration testing with wiremock.
 */
public class JavaUrlUpdaterMock extends JavaUrlUpdater {

  JavaUrlUpdaterMock(String baseUrl) {

    super(baseUrl);
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/versions/";
  }
}
