package com.devonfw.tools.ide.integrationtest.python;

import com.devonfw.tools.ide.url.updater.python.PythonUrlUpdater;

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
