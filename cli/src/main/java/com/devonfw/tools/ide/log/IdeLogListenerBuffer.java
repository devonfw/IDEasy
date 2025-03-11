package com.devonfw.tools.ide.log;

import java.util.List;

/**
 * Extends {@link IdeLogListenerCollector} to buffer log events during bootstrapping and then flush them once the logger is properly configured.
 */
public class IdeLogListenerBuffer extends IdeLogListenerCollector {

  @Override
  public boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {

    if (this.entries == null) {
      return true;
    } else {
      // buffer the log event
      super.onLog(level, message, rawMessage, args, error);
      // reject further processing of the log event suppressing it (so it is only cached)
      return false;
    }
  }

  /**
   * This method is supposed to be called once after the {@link IdeLogger} has been properly initialized.
   *
   * @param logger the initialized {@link IdeLogger}.
   */
  public void flushAndDisable(IdeLogger logger) {

    if (this.entries == null) {
      return;
    }
    List<IdeLogEntry> buffer = this.entries;
    // disable ourselves from collecting further events
    this.entries = null;
    // write all cached log events to the logger again for processing
    for (IdeLogEntry entry : buffer) {
      logger.level(entry.level()).log(entry.error(), entry.message());
    }
  }

}
