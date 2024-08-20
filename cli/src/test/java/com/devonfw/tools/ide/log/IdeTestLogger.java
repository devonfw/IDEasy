package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@link IdeLoggerImpl} for testing.
 */
public class IdeTestLogger extends IdeLoggerImpl {

  private final List<IdeLogEntry> entries;

  public IdeTestLogger() {

    this(IdeLogLevel.DEBUG);
  }

  public IdeTestLogger(IdeLogLevel minLogLevel) {

    this(new ArrayList<>(), minLogLevel);
  }

  private IdeTestLogger(List<IdeLogEntry> entries, IdeLogLevel minLogLevel) {

    super(minLogLevel, level -> new IdeSubLoggerTest(level, entries));
    this.entries = entries;
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.entries;
  }
}
