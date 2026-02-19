package com.devonfw.tools.ide.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Implementation of {@link SLF4JServiceProvider}.
 */
public class Slf4jProviderIdeasy implements SLF4JServiceProvider {

  private final Slf4jLoggerFactoryIdeasy loggerFactory;

  /**
   * The constructor.
   */
  public Slf4jProviderIdeasy() {
    super();
    this.loggerFactory = new Slf4jLoggerFactoryIdeasy();
  }

  @Override
  public ILoggerFactory getLoggerFactory() {

    return this.loggerFactory;
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

    return "2.0.12";
  }

  @Override
  public void initialize() {
    // nothing to do...
  }
}
