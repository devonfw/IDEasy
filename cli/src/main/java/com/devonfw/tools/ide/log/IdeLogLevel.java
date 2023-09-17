package com.devonfw.tools.ide.log;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Enum} with the available log-levels.
 *
 * @see IdeContext#level(IdeLogLevel)
 */
public enum IdeLogLevel {

  /** {@link IdeLogLevel} for tracing (very detailed and verbose logging). */
  TRACE("\033[38;5;240m"),

  /** {@link IdeLogLevel} for debugging (more detailed logging). */
  DEBUG("\033[90m"),

  /** {@link IdeLogLevel} for general information (regular logging). */
  INFO("\033[39m"),

  /**
   * {@link IdeLogLevel} for a step (logs the step name and groups the following log statements until the next step).
   */
  STEP("\033[35m"),

  /** {@link IdeLogLevel} for user interaction (e.g. questions or options). */
  INTERACTION("\033[96m"),

  /** {@link IdeLogLevel} for success (an important aspect has been completed successfully). */
  SUCCESS("\033[92m"),

  /** {@link IdeLogLevel} for a warning (something unexpected or abnormal happened but can be compensated). */
  WARNING("\033[93m"),

  /**
   * {@link IdeLogLevel} for an error (something failed and we cannot proceed or the user has to continue with extreme
   * care).
   */
  ERROR("\033[91m");

  private final String color;

  /**
   * The constructor.
   */
  private IdeLogLevel(String color) {

    this.color = color;
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

}
