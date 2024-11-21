package com.devonfw.tools.ide.log;

/**
 * Interface to listen for log-events.
 */
@FunctionalInterface
public interface IdeLogListener {

  /**
   * @param level the {@link IdeLogLevel}.
   * @param message the actual message to be logged.
   * @param rawMessage the raw message logged (without args filled in)
   * @param args the optional message arguments.
   * @param error the optional error.
   * @return {@code true} to accept this log event (default), {@code false} otherwise to reject further processing and suppress the log message.
   */
  boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error);

}
