package com.devonfw.tools.ide.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Implementation of {@link ILoggerFactory}.
 */
public class Slf4jLoggerFactoryIdeasy implements ILoggerFactory {

  @Override
  public Logger getLogger(String name) {

    return new Slf4jLoggerAdapter(name);
  }
}
