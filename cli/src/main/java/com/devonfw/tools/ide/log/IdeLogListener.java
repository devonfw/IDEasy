package com.devonfw.tools.ide.log;

/**
 * Interface to listen for log-events.
 */
@FunctionalInterface
public interface IdeLogListener {

  /**
   * @param entry the {@link IdeLogEntry} to log.
   * @return {@code true} to accept this log event (default), {@code false} otherwise to reject further processing and suppress the log message.
   */
  boolean onLog(IdeLogEntry entry);

}
