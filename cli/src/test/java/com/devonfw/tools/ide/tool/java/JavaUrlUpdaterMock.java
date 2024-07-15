package com.devonfw.tools.ide.tool.java;

/**
 * Mock of {@link JavaUrlUpdater} to allow integration testing with wiremock.
 */
public class JavaUrlUpdaterMock extends JavaUrlUpdater {

  private final static String TEST_URL = "http://localhost:8080/";

  @Override
  protected String getMirror() {

    return TEST_URL + "downloads/";
  }

  @Override
  protected String doGetVersionUrl() {

    return TEST_URL + "versions/";
  }
}
