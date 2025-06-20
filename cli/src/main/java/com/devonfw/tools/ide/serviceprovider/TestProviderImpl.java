package com.devonfw.tools.ide.serviceprovider;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Implementation of {@link SLF4JServiceProvider}.
 */
public class TestProviderImpl implements SLF4JServiceProvider {

  private final String REQUESTED_API_VERSION = "2.0.12";
  private TestLoggerFactoryImpl testLoggerFactory;
  

  @Override
  public ILoggerFactory getLoggerFactory() {

    return testLoggerFactory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {

    return null;
  }

  @Override
  public MDCAdapter getMDCAdapter() {

    return new NOPMDCAdapter();
  }

  @Override
  public String getRequestedApiVersion() {

    return REQUESTED_API_VERSION;
  }

  @Override
  public void initialize() {
    testLoggerFactory = new TestLoggerFactoryImpl();
  }
}
