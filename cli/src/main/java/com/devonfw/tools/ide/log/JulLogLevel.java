package com.devonfw.tools.ide.log;

import java.util.logging.Level;

/**
 * {@link Level} from java.util.logging corresponding to {@link IdeLogLevel}.
 */
public class JulLogLevel extends Level {

  /** @see IdeLogLevel#TRACE */
  public static final Level TRACE = new JulLogLevel("TRACE", 401);

  /** @see IdeLogLevel#DEBUG */
  public static final Level DEBUG = new JulLogLevel("DEBUG", 501);

  /** @see IdeLogLevel#INFO */
  public static final Level INFO = Level.INFO;

  /** @see IdeLogLevel#STEP */
  public static final Level STEP = new JulLogLevel("STEP", 801);

  /** @see IdeLogLevel#INTERACTION */
  public static final Level INTERACTION = new JulLogLevel("INTERACTION", 802);

  /** @see IdeLogLevel#SUCCESS */
  public static final Level SUCCESS = new JulLogLevel("SUCCESS", 803);

  /** @see IdeLogLevel#WARNING */
  public static final Level WARNING = Level.WARNING;

  /** @see IdeLogLevel#WARNING */
  public static final Level ERROR = new JulLogLevel("ERROR", 1001);

  /** @see IdeLogLevel#PROCESSABLE */
  public static final Level PROCESSABLE = new JulLogLevel("PROCESSABLE", 2000);

  private JulLogLevel(String name, int value) {

    super(name, value);
  }

  /**
   * Ensures that all custom log-levels are initialized.
   */
  public static void init() {

  }
}
