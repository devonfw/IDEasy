package com.devonfw.tools.ide.serviceprovider;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Implementation of {@link ILoggerFactory}.
 */
public class TestLoggerFactoryImpl implements ILoggerFactory {

  @Override
  public Logger getLogger(String name) {

    return new IdeLoggerAdapter(name);
  }
}
