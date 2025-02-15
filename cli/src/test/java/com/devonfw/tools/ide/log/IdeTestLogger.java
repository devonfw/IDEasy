package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.context.IdeStartContextImpl;

/**
 * Extends {@link IdeStartContextImpl} for testing.
 */
public class IdeTestLogger extends IdeStartContextImpl {

  private final IdeLogListenerCollector collector;

  public IdeTestLogger() {

    this(IdeLogLevel.DEBUG);
  }

  public IdeTestLogger(IdeLogLevel minLogLevel) {

    this(new ArrayList<>(), minLogLevel, new IdeLogListenerCollector());
  }

  private IdeTestLogger(List<IdeLogEntry> entries, IdeLogLevel minLogLevel, IdeLogListenerCollector collector) {

    super(minLogLevel, level -> new IdeSubLoggerOut(level, null, true, minLogLevel, collector));
    this.collector = collector;
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.collector.getEntries();
  }
}
