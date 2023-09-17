package com.devonfw.tools.ide.log;

/**
 * Interface for a logger to {@link #log(String) log a message} on a specific {@link #getLevel() log-level}.
 */
public interface IdeSubLogger {

  /**
   * @param message the message to log.
   */
  void log(String message);

  /**
   * @param message the message to log. Should contain "{}" as placeholder for the given arguments.
   * @param args the dynamic arguments to fill in.
   */
  default void log(String message, Object... args) {

    assert ((args.length == 0)
        || !(args[0] instanceof Throwable)) : "Throwable has to be first argument before message!";
    if (!isEnabled()) {
      return;
    }
    int pos = message.indexOf("{}");
    if ((pos < 0) || (args.length == 0)) {
      log(message);
      return;
    }
    int argIndex = 0;
    int start = 0;
    int length = message.length();
    StringBuilder sb = new StringBuilder(length + 48);
    while (pos >= 0) {
      sb.append(message, start, pos);
      sb.append(args[argIndex++]);
      start = pos + 2;
      if (argIndex < args.length) {
        pos = message.indexOf("{}", start);
      } else {
        pos = -1;
      }
    }
    if (start < length) {
      String rest = message.substring(start);
      sb.append(rest);
    }
    log(sb.toString());
  }

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
