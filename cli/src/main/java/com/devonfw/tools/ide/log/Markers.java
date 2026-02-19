package com.devonfw.tools.ide.log;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Markers for SLF4J to emulate IDEasy custom log-levels. Always use log-level INFO on SLF4J in combination with the {@link Markers} defined here.
 */
public final class Markers {

  private Markers() {

  }

  /** {@link Marker} for {@link IdeLogLevel#INTERACTION}. */
  public static final Marker INTERACTION = MarkerFactory.getMarker("interaction");

  /** {@link Marker} for {@link IdeLogLevel#STEP}. */
  public static final Marker STEP = MarkerFactory.getMarker("step");

  /** {@link Marker} for {@link IdeLogLevel#SUCCESS}. */
  public static final Marker SUCCESS = MarkerFactory.getMarker("success");

  /** {@link Marker} for {@link IdeLogLevel#PROCESSABLE}. */
  public static final Marker PROCESSABLE = MarkerFactory.getMarker("processable");

  /**
   * @param marker the {@link Marker}.
   * @return the corresponding {@link IdeLogLevel}.
   */
  public static IdeLogLevel getLevel(Marker marker) {

    if (marker == INTERACTION) {
      return IdeLogLevel.INTERACTION;
    } else if (marker == STEP) {
      return IdeLogLevel.STEP;
    } else if (marker == SUCCESS) {
      return IdeLogLevel.SUCCESS;
    } else if (marker == PROCESSABLE) {
      return IdeLogLevel.PROCESSABLE;
    } else {
      return IdeLogLevel.INFO; // unknown marker
    }
  }

}
