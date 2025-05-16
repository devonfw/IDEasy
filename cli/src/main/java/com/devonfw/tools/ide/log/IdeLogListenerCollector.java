package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link IdeLogListener} that collects all events as {@link IdeLogEntry}.
 */
public class IdeLogListenerCollector implements IdeLogListener {

  protected final List<IdeLogEntry> entries;

  protected IdeLogLevel threshold;

  /**
   * The constructor.
   */
  public IdeLogListenerCollector() {
    super();
    this.entries = new ArrayList<>(512);
    this.threshold = IdeLogLevel.TRACE;
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.entries;
  }

  @Override
  public boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {
    if (level.ordinal() >= threshold.ordinal()) {
      this.entries.add(new IdeLogEntry(level, message, rawMessage, args, error));
    }
    return true;
  }

  /**
   * @return {@code true} if this collector is active and collects all logs, {@code false} otherwise (disabled and no filtering of logs so regular logging).
   */
  protected boolean isActive() {
    return true;
  }
}
