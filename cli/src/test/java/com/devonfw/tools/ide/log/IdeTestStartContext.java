package com.devonfw.tools.ide.log;

import java.util.List;

import com.devonfw.tools.ide.context.IdeStartContextImpl;

/**
 * Extends {@link IdeStartContextImpl} for testing.
 */
public class IdeTestStartContext extends IdeStartContextImpl {

  private final IdeLogListenerCollector collector;

  public IdeTestStartContext(IdeLogLevel logLevel) {

    this(logLevel, new IdeLogListenerCollector());
  }

  private IdeTestStartContext(IdeLogLevel minLogLevel, IdeLogListenerCollector logListener) {

    super(minLogLevel, logListener);
    this.collector = logListener;
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.collector.getEntries();
  }
}
