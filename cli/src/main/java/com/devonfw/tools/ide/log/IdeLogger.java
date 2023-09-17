package com.devonfw.tools.ide.log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Interface for interaction with the user allowing to input and output information.
 */
public interface IdeLogger {

  /**
   * @param level the {@link IdeLogLevel}.
   * @return the requested {@link IdeLogLevel} for the given {@link IdeLogLevel}.
   * @see IdeSubLogger#getLevel()
   */
  IdeSubLogger level(IdeLogLevel level);

  /**
   * @return the {@link #level(IdeLogLevel) logger} for {@link IdeLogLevel#TRACE}.
   */
  default IdeSubLogger trace() {

    return level(IdeLogLevel.TRACE);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String) message to log} with {@link IdeLogLevel#TRACE}.
   */
  default void trace(String message) {

    trace().log(message);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#TRACE}.
   * @param args the dynamic arguments to fill in.
   */
  default void trace(String message, Object... args) {

    trace().log(message, args);
  }

  /**
   * @return the {@link #level(IdeLogLevel) logger} for {@link IdeLogLevel#DEBUG}.
   */
  default IdeSubLogger debug() {

    return level(IdeLogLevel.DEBUG);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String) message to log} with {@link IdeLogLevel#DEBUG}.
   */
  default void debug(String message) {

    debug().log(message);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#DEBUG}.
   * @param args the dynamic arguments to fill in.
   */
  default void debug(String message, Object... args) {

    debug().log(message, args);
  }

  /**
   * @return the {@link #level(IdeLogLevel) logger} for {@link IdeLogLevel#INFO}.
   */
  default IdeSubLogger info() {

    return level(IdeLogLevel.INFO);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String) message to log} with {@link IdeLogLevel#INFO}.
   */
  default void info(String message) {

    info().log(message);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#INFO}.
   * @param args the dynamic arguments to fill in.
   */
  default void info(String message, Object... args) {

    info().log(message, args);
  }

  /**
   * @return the {@link #level(IdeLogLevel) logger} for {@link IdeLogLevel#STEP}.
   */
  default IdeSubLogger step() {

    return level(IdeLogLevel.STEP);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String) message to log} with {@link IdeLogLevel#STEP}.
   */
  default void step(String message) {

    step().log(message);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#STEP}.
   * @param args the dynamic arguments to fill in.
   */
  default void step(String message, Object... args) {

    step().log(message, args);
  }

  /**
   * @return the {@link #level(IdeLogLevel) logger} for {@link IdeLogLevel#INTERACTION}.
   */
  default IdeSubLogger interaction() {

    return level(IdeLogLevel.INTERACTION);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String) message to log} with {@link IdeLogLevel#INTERACTION}.
   */
  default void interaction(String message) {

    interaction().log(message);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#INTERACTION}.
   * @param args the dynamic arguments to fill in.
   */
  default void interaction(String message, Object... args) {

    interaction().log(message, args);
  }

  /**
   * @return the {@link #level(IdeLogLevel) logger} for {@link IdeLogLevel#SUCCESS}.
   */
  default IdeSubLogger success() {

    return level(IdeLogLevel.SUCCESS);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String) message to log} with {@link IdeLogLevel#SUCCESS}.
   */
  default void success(String message) {

    success().log(message);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#SUCCESS}.
   * @param args the dynamic arguments to fill in.
   */
  default void success(String message, Object... args) {

    success().log(message, args);
  }

  /**
   * @return the {@link #level(IdeLogLevel) logger} for {@link IdeLogLevel#WARNING}.
   */
  default IdeSubLogger warning() {

    return level(IdeLogLevel.WARNING);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String) message to log} with {@link IdeLogLevel#WARNING}.
   */
  default void warning(String message) {

    warning().log(message);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#WARNING}.
   * @param args the dynamic arguments to fill in.
   */
  default void warning(String message, Object... args) {

    warning().log(message, args);
  }

  /**
   * @return the {@link #level(IdeLogLevel) logger} for {@link IdeLogLevel#ERROR}.
   */
  default IdeSubLogger error() {

    return level(IdeLogLevel.ERROR);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String) message to log} with {@link IdeLogLevel#ERROR}.
   */
  default void error(String message) {

    error().log(message);
  }

  /**
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#ERROR}.
   * @param args the dynamic arguments to fill in.
   */
  default void error(String message, Object... args) {

    error().log(message, args);
  }

  /**
   * @param error the {@link Throwable} that caused the error.
   */
  default void error(Throwable error) {

    error(error, null);
  }

  /**
   * @param error the {@link Throwable} that caused the error.
   * @param message the {@link IdeSubLogger#log(String, Object...) message to log} with {@link IdeLogLevel#ERROR}.
   */
  default void error(Throwable error, String message) {

    boolean hasMessage = !isBlank(message);
    if (error == null) {
      if (hasMessage) {
        error(message);
      } else {
        error("Internal error: Throwable is null!");
      }
      return;
    }
    boolean traceEnabled = true; // trace().isEnabled();
    boolean debugEnabled = debug().isEnabled();
    // if (hasMessage && !debugEnabled) {
    // // assuming that if debug is disabled then also trace is disabled so we ignore error
    // error(message);
    // return;
    // }
    String errorMessage = error.getMessage();
    if (errorMessage == null) {
      errorMessage = "";
    } else if (errorMessage.isBlank()) {
      errorMessage = error.getClass().getName();
    }
    int capacity = 0;
    if (hasMessage) {
      capacity = message.length();
    } else if (!debugEnabled) {
      capacity = errorMessage.length();
    }
    if (debugEnabled) {
      capacity = capacity + 32 + errorMessage.length();
    }
    if (traceEnabled) {
      capacity = capacity + 512;
    }
    StringWriter sw = new StringWriter(capacity);
    if (hasMessage) {
      sw.append(message);
    }
    if (traceEnabled) {
      sw.append('\n');
      try (PrintWriter pw = new PrintWriter(sw)) {
        error.printStackTrace(pw);
      }
    } else if (debugEnabled) {
      sw.append('\n');
      sw.append(error.toString());
    } else {
      sw.append(errorMessage);
    }
    error(sw.toString());
  }

  private static boolean isBlank(String string) {

    if ((string == null) || (string.isBlank())) {
      return true;
    }
    return false;
  }

}
