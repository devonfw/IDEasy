package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link IdeLogListener} that collects all events as {@link IdeLogEntry}.
 */
public class IdeLogListenerCollector extends IdeLogListenerBuffer {

  protected final List<IdeLogEntry> entries;

  /**
   * The constructor.
   */
  public IdeLogListenerCollector() {
    super(false);
    this.entries = new ArrayList<>(512);
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.entries;
  }

  @Override
  public boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {
    boolean accept = super.onLog(level, message, rawMessage, args, error);
    if (accept) {
      this.entries.add(new IdeLogEntry(level, message, rawMessage, args, error));
    }
    return accept;
  }

}
