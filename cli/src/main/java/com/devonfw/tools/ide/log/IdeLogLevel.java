package com.devonfw.tools.ide.log;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Enum} with the available log-levels.
 *
 * @see IdeContext#level(IdeLogLevel)
 */
public enum IdeLogLevel {

  /** {@link IdeLogLevel} for tracing (very detailed and verbose logging). */
  TRACE("\033[38;5;240m", Level.TRACE, null, java.util.logging.Level.FINER),

  /** {@link IdeLogLevel} for debugging (more detailed logging). */
  DEBUG("\033[90m", Level.DEBUG, null, java.util.logging.Level.FINE),

  /** {@link IdeLogLevel} for general information (regular logging). */
  INFO(null, Level.INFO, null, java.util.logging.Level.INFO),

  /**
   * {@link IdeLogLevel} for a step (logs the step name and groups the following log statements until the next step).
   */
  STEP("\033[35m", Level.INFO, MarkerFactory.getMarker("step"), java.util.logging.Level.INFO),

  /** {@link IdeLogLevel} for user interaction (e.g. questions or options). */
  INTERACTION("\033[96m", Level.INFO, MarkerFactory.getMarker("interaction"), java.util.logging.Level.INFO),

  /** {@link IdeLogLevel} for success (an important aspect has been completed successfully). */
  SUCCESS("\033[92m", Level.INFO, MarkerFactory.getMarker("success"), java.util.logging.Level.INFO),

  /** {@link IdeLogLevel} for a warning (something unexpected or abnormal happened but can be compensated). */
  WARNING("\033[93m", Level.WARN, null, java.util.logging.Level.WARNING),

  /**
   * {@link IdeLogLevel} for an error (something failed and we cannot proceed or the user has to continue with extreme care).
   */
  ERROR("\033[91m", Level.ERROR, null, java.util.logging.Level.SEVERE),

  /** {@link IdeLogLevel} for {@link com.devonfw.tools.ide.commandlet.Commandlet#isProcessableOutput() processable output} */
  PROCESSABLE(null, Level.INFO, MarkerFactory.getMarker("processable"), java.util.logging.Level.INFO);

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

    return (this == STEP) || (this == INTERACTION) || (this == SUCCESS) || (this == PROCESSABLE);
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
   * @param level the SLF4J log {@link Level}.
   * @param marker the SLF4J {@link Marker}.
   * @return the {@link IdeLogLevel}.
   */
  public static java.util.logging.Level convertLevelFromSlf4jToJul(Level level, Marker marker) {

    return switch (level) {
      case ERROR -> java.util.logging.Level.SEVERE;
      case WARN -> java.util.logging.Level.WARNING;
      case INFO -> java.util.logging.Level.INFO;
      case DEBUG -> java.util.logging.Level.FINE;
      case TRACE -> java.util.logging.Level.FINER;
      default -> throw new IllegalStateException("" + level);
    };
  }
}
