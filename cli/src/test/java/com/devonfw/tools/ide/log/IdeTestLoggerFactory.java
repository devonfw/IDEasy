package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Factory for {@link IdeTestLogger}.
 */
public class IdeTestLoggerFactory implements Function<IdeLogLevel, IdeSubLogger> {

  private final IdeLogLevel logLevel;

  private final List<IdeLogEntry> entries;

  /**
   * The constructor.
   */
  public IdeTestLoggerFactory() {
    this(IdeLogLevel.TRACE);
  }

  /**
   * The constructor.
   *
   * @param logLevel the {@link IdeLogLevel} used as threshold for logging.
   */
  public IdeTestLoggerFactory(IdeLogLevel logLevel) {
    super();
    this.logLevel = logLevel;
    this.entries = new ArrayList<>(512);
  }

  @Override
  public IdeSubLogger apply(IdeLogLevel ideLogLevel) {

    if (ideLogLevel.ordinal() < this.logLevel.ordinal()) {
      return new IdeSubLoggerNone(ideLogLevel);
    }
    return new IdeTestLogger(ideLogLevel, this.entries);
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.entries;
  }

}
