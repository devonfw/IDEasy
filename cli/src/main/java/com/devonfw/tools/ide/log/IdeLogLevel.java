package com.devonfw.tools.ide.log;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import com.devonfw.tools.ide.context.IdeStartContextImpl;

/**
 * {@link Enum} with the available log-levels for IDEasy.
 *
 * @see Slf4jLoggerAdapter
 */
public enum IdeLogLevel {

  /** {@link IdeLogLevel} for tracing (very detailed and verbose logging). */
  TRACE("\033[38;5;240m", Level.TRACE, null, JulLogLevel.TRACE),

  /** {@link IdeLogLevel} for debugging (more detailed logging). */
  DEBUG("\033[90m", Level.DEBUG, null, JulLogLevel.DEBUG),

  /** {@link IdeLogLevel} for general information (regular logging). */
  INFO(null, Level.INFO, null, JulLogLevel.INFO),

  /**
   * {@link IdeLogLevel} for a step (logs the step name and groups the following log statements until the next step).
   */
  STEP("\033[35m", Level.INFO, MarkerFactory.getMarker("STEP"), JulLogLevel.STEP),

  /** {@link IdeLogLevel} for user interaction (e.g. questions or options). */
  INTERACTION("\033[96m", Level.INFO, MarkerFactory.getMarker("INTERACTION"), JulLogLevel.INTERACTION),

  /** {@link IdeLogLevel} for success (an important aspect has been completed successfully). */
  SUCCESS("\033[92m", Level.INFO, MarkerFactory.getMarker("SUCCESS"), JulLogLevel.SUCCESS),

  /** {@link IdeLogLevel} for a warning (something unexpected or abnormal happened but can be compensated). */
  WARNING("\033[93m", Level.WARN, null, JulLogLevel.WARNING),

  /**
   * {@link IdeLogLevel} for an error (something failed and we cannot proceed or the user has to continue with extreme care).
   */
  ERROR("\033[91m", Level.ERROR, null, JulLogLevel.ERROR),

  /** {@link IdeLogLevel} for {@link com.devonfw.tools.ide.commandlet.Commandlet#isProcessableOutput() processable output} */
  PROCESSABLE(null, Level.INFO, MarkerFactory.getMarker("PROCESSABLE"), JulLogLevel.PROCESSABLE);

  private final String color;

  private final Level slf4jLevel;

  private final Marker slf4jMarker;

  private final java.util.logging.Level julLevel;

  /**
   * The constructor.
   */
  private IdeLogLevel(String color, Level slf4jLevel, Marker slf4jMarker, java.util.logging.Level julLevel) {

    this.color = color;
    this.slf4jLevel = slf4jLevel;
    this.slf4jMarker = slf4jMarker;
    this.julLevel = julLevel;
  }

  /**
   * @return the prefix to append for colored output to set color according to this {@link IdeLogLevel}.
   */
  public String getStartColor() {

    return this.color;
  }

  /**
   * @return the suffix to append for colored output to reset to default color.
   */
  public String getEndColor() {

    return "\033[0m"; // reset color
  }

  /**
   * @return the Slf4J {@link Level}.
   */
  public Level getSlf4jLevel() {

    return this.slf4jLevel;
  }

  /**
   * @return the SLF4J {@link Marker}. Will be {@code null} for standard log-levels.
   */
  public Marker getSlf4jMarker() {

    return this.slf4jMarker;
  }

  /**
   * @return the JUL {@link java.util.logging.Level}.
   */
  public java.util.logging.Level getJulLevel() {

    return this.julLevel;
  }

  /**
   * @return {@code true} in case of a custom log-level, {@code false} otherwise (standard log-level supported by SLF4J and all reasonable loggers).
   */
  public boolean isCustom() {

    return (this.slf4jMarker != null);
  }

  /**
   * @param logger the SLF4J {@link Logger}.
   * @param error the {@link Throwable} with the error to log. Must not be {@code null}.
   */
  public void log(Logger logger, Throwable error) {

    log(logger, error, null, (Object[]) null);
  }

  /**
   * @param logger the SLF4J {@link Logger}.
   * @param error the optional {@link Throwable} with the error to log or {@code null} for no error.
   * @param message the message (template) to log.
   */
  public void log(Logger logger, Throwable error, String message) {

    log(logger, error, message, (Object[]) null);
  }

  /**
   * @param logger the SLF4J {@link Logger}.
   * @param message the message (template) to log.
   */
  public void log(Logger logger, String message) {

    log(logger, null, message, (Object[]) null);
  }

  /**
   * @param logger the SLF4J {@link Logger}.
   * @param message the message (template) to log.
   * @param args the optional arguments to fill into the {@code message}. May be {@code null} or empty for no parameters.
   */
  public void log(Logger logger, String message, Object... args) {

    log(logger, null, message, args);
  }

  /**
   * @param logger the SLF4J {@link Logger}.
   * @param error the optional {@link Throwable} with the error to log or {@code null} for no error.
   * @param message the message (template) to log.
   * @param args the optional arguments to fill into the {@code message}. May be {@code null} or empty for no parameters.
   */
  public void log(Logger logger, Throwable error, String message, Object... args) {

    LoggingEventBuilder builder = logger.atLevel(this.slf4jLevel).setCause(error);
    if (this.slf4jMarker != null) {
      builder = builder.addMarker(this.slf4jMarker);
    }
    if (Slf4jLoggerAdapter.isEmpty(args)) {
      builder.log(message, args);
    } else if (message == null) {
      String msg = error.getMessage();
      if (msg == null) {
        msg = error.toString();
      }
      builder.log(msg);
    } else {
      builder.log(message);
    }
  }

  /**
   * @return {@code true} if this {@link IdeLogLevel} is enabled (globally), {@code false} otherwise.
   */
  public boolean isEnabled() {

    IdeLogLevel threshold = getLogLevel();
    return ordinal() >= threshold.ordinal();
  }

  static IdeLogLevel getLogLevel() {
    IdeLogLevel threshold = IdeLogLevel.TRACE;
    IdeStartContextImpl startContext = IdeStartContextImpl.get();
    if (startContext != null) {
      threshold = startContext.getLogLevel();
    }
    return threshold;
  }

  /**
   * @param marker the {@link Marker}.
   * @return the corresponding {@link IdeLogLevel}.
   */
  public static IdeLogLevel getLevel(Marker marker) {

    if (marker == INTERACTION.slf4jMarker) {
      return IdeLogLevel.INTERACTION;
    } else if (marker == STEP.slf4jMarker) {
      return IdeLogLevel.STEP;
    } else if (marker == SUCCESS.slf4jMarker) {
      return IdeLogLevel.SUCCESS;
    } else if (marker == PROCESSABLE.slf4jMarker) {
      return IdeLogLevel.PROCESSABLE;
    } else {
      return IdeLogLevel.INFO; // unknown marker
    }
  }

  /**
   * @param level the SLF4J log {@link Level}.
   * @param marker the SLF4J {@link Marker}.
   * @return the {@link IdeLogLevel}.
   */
  public static IdeLogLevel of(Level level, Marker marker) {

    return switch (level) {
      case ERROR -> IdeLogLevel.ERROR;
      case WARN -> IdeLogLevel.WARNING;
      case INFO -> getLevel(marker);
      case DEBUG -> IdeLogLevel.DEBUG;
      case TRACE -> IdeLogLevel.TRACE;
      default -> throw new IllegalStateException("" + level);
    };
  }

  /**
   * @param level the JUL {@link Level}.
   * @return the {@link IdeLogLevel}.
   */
  public static IdeLogLevel of(java.util.logging.Level level) {

    for (IdeLogLevel ideLevel : values()) {
      if (ideLevel.julLevel == level) {
        return ideLevel;
      }
    }
    throw new IllegalStateException("" + level);
  }

}
