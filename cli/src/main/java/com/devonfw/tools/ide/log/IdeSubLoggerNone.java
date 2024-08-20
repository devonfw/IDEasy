package com.devonfw.tools.ide.log;

/**
 * Implementation of {@link IdeSubLogger} that is NOT {@link #isEnabled() enabled} and does nothing.
 */
public final class IdeSubLoggerNone extends AbstractIdeSubLogger {

  private static final IdeSubLoggerNone[] LOGGERS;

  static {
    IdeLogLevel[] levels = IdeLogLevel.values();
    LOGGERS = new IdeSubLoggerNone[levels.length];
    for (int i = 0; i < levels.length; i++) {
      LOGGERS[i] = new IdeSubLoggerNone(levels[i]);
    }
  }

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  private IdeSubLoggerNone(IdeLogLevel level) {

    super(level);
  }

  @Override
  public String log(Throwable error, String message, Object... args) {

    return message;
  }

  @Override
  public boolean isEnabled() {

    return false;
  }

  /**
   * @param level the {@link IdeLogLevel}.
   * @return the {@link IdeSubLoggerNone} instance.
   */
  public static IdeSubLoggerNone of(IdeLogLevel level) {

    return LOGGERS[level.ordinal()];
  }

}
