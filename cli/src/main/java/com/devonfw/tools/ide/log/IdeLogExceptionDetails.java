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
      if ((errorMessage == null) || errorMessage.isBlank()) {
        errorMessage = error.getClass().getName();
      }
      sw.append(errorMessage);
    }
  },

  /** Ignore error and only log explicit message. */
  NONE(0) {
    @Override
    String format(String message, Throwable error) {
      return message;
    }

    @Override
    void format(Throwable error, StringWriter sw) {

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

    int capacity = this.capacityOffset;
    if (message != null) {
      capacity = capacity + message.length() + 1;
    }
    StringWriter sw = new StringWriter(capacity);
    if (message != null) {
      sw.append(message);
      sw.append('\n');
    }
    format(error, sw);
    return sw.toString();
  }

  abstract void format(Throwable error, StringWriter sw);

  /**
   * @param level the {@link IdeLogLevel} of the {@link IdeSubLogger}.
   * @param minLogLevel the minimum {@link IdeLogLevel} (threshold).
   * @return the {@link IdeLogExceptionDetails}.
   */
  static IdeLogExceptionDetails of(IdeLogLevel level, IdeLogLevel minLogLevel) {

    if ((minLogLevel == IdeLogLevel.TRACE) || (minLogLevel == IdeLogLevel.DEBUG)) {
      return STACKTRACE;
    }
    return switch (level) {
      case ERROR -> STACKTRACE;
      case WARNING -> TO_STRING;
      default -> MESSAGE;
    };
  }

}
