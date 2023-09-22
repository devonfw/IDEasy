package com.devonfw.tools.ide.tool.python;

/**
 * {@Link JsonUrlUpdater} test mock for Python
 */
public class PythonUrlUpdaterMock extends PythonUrlUpdater {
  private final static String TEST_BASE_URL = "http://localhost:8080";

  @Override
  protected String getVersionBaseUrl() {

    return TEST_BASE_URL;
  }
}
