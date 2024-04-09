package com.devonfw.tools.ide.log;

/**
 * Interface for a logger to {@link #log(String) log a message} on a specific {@link #getLevel() log-level}.
 */
public interface IdeSubLogger {

  /**
   * @param message the message to log.
   */
  default void log(String message) {

    log(null, message);
  }

  /**
   * @param error the {@link Throwable} that was catched and should be logged or {@code null} for no error.
   * @param message the message to log.
   * @param args the dynamic arguments to fill in.
   */
  default void log(String message, Object... args) {

    log(null, message, args);
  }

  /**
   * @param error the {@link Throwable} that was catched and should be logged or {@code null} for no error.
   * @param message the message to log.
   */
  default void log(Throwable error, String message) {

    log(error, message, (Object[]) null);
  }

  /**
   * @param error the {@link Throwable} that was catched and should be logged or {@code null} for no error.
   * @param message the message to log.
   * @param args the dynamic arguments to fill in.
   */
  void log(Throwable error, String message, Object... args);

  /**
   * @return {@code true} if this logger is enabled, {@code false} otherwise (this logger does nothing and all
   *         {@link #log(String) logged messages} with be ignored).
   */
  boolean isEnabled();

  /**
   * @return the {@link IdeLogLevel} of this logger.
   */
  IdeLogLevel getLevel();

}
