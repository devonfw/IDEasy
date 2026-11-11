package com.devonfw.tools.ide.log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
    this.entries = new CopyOnWriteArrayList<>();
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.entries;
  }

  @Override
  public boolean onLog(IdeLogEntry entry) {
    boolean accept = super.onLog(entry);
    if (accept) {
      this.entries.add(entry);
    }
    return accept;
  }

}
