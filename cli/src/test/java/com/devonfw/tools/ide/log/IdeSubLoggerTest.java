package com.devonfw.tools.ide.log;

import java.util.List;

/**
 * Implementation of {@link IdeSubLogger} for testing that collects all messages and allows to check if an expected message was logged.
 */
public class IdeSubLoggerTest extends IdeSubLoggerSlf4j {

  private final List<IdeLogEntry> entries;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  public IdeSubLoggerTest(IdeLogLevel level, List<IdeLogEntry> entries) {

    super(level);
    this.entries = entries;
  }

  @Override
  public String log(Throwable error, String message, Object... args) {

    String result = super.log(error, message, args);
    this.entries.add(new IdeLogEntry(level, result));
    return result;
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.entries;
  }

  @Override
  public boolean isEnabled() {

    return true;
  }

}
