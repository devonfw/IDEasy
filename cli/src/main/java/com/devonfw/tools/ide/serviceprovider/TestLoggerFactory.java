package com.devonfw.tools.ide.serviceprovider;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class TestLoggerFactory implements ILoggerFactory {

  @Override
  public Logger getLogger(String name) {

    return new TestLogger(name);
  }
}
