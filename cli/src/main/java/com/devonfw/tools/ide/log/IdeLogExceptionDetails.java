package com.devonfw.tools.ide.log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * {@link Enum} with the available details logged for an {@link Throwable error}.
 */
enum IdeLogExceptionDetails {

  /** Log the entire stacktrace. */
  STACKTRACE(512) {

    @Override
    void format(Throwable error, StringWriter sw) {

      try (PrintWriter pw = new PrintWriter(sw)) {
        error.printStackTrace(pw);
      }
    }
  },

  /** Log only the exception type and message. */
  TO_STRING(32) {

    @Override
    void format(Throwable error, StringWriter sw) {

      sw.append(error.toString());
    }
  },

  /** Log only the message. */
  MESSAGE(16) {

    @Override
    void format(Throwable error, StringWriter sw) {

      String errorMessage = error.getMessage();
      if (isBlank(errorMessage)) {
        errorMessage = error.getClass().getName();
      }
      sw.append(errorMessage);
    }
  };

  private final int capacityOffset;

  private IdeLogExceptionDetails(int capacityOffset) {

    this.capacityOffset = capacityOffset;
  }

  /**
   * @param message the formatted log message.
   * @param error the {@link Throwable} to log.
   */
  String format(String message, Throwable error) {

    boolean hasMessage = !isBlank(message);
    if (error == null) {
      if (hasMessage) {
        return message;
      } else {
        return "Internal error: Both message and error is null - nothing to log!";
      }
    }
    int capacity = this.capacityOffset;
    if (hasMessage) {
      capacity = capacity + message.length() + 1;
    }
    StringWriter sw = new StringWriter(capacity);
    if (hasMessage) {
      sw.append(message);
      sw.append('\n');
    }
    format(error, sw);
    return sw.toString();
  }

  abstract void format(Throwable error, StringWriter sw);

  private static boolean isBlank(String string) {

    if ((string == null) || (string.isBlank())) {
      return true;
    }
    return false;
  }

  /**
   * @param level the {@link IdeLogLevel} of the {@link IdeSubLogger}.
   * @param minLogLevel the minimum {@link IdeLogLevel} (threshold).
   * @return the {@link IdeLogExceptionDetails}.
   */
  static IdeLogExceptionDetails of(IdeLogLevel level, IdeLogLevel minLogLevel) {

    if ((minLogLevel == IdeLogLevel.TRACE) || (minLogLevel == IdeLogLevel.DEBUG)) {
      return STACKTRACE;
    }
    switch (level) {
      case ERROR:
        return STACKTRACE;
      case WARNING:
        return TO_STRING;
      default:
        return MESSAGE;
    }
  }

}
