package com.devonfw.tools.ide.log;

/**
 * Implementation of {@link IdeSubLogger} that is NOT {@link #isEnabled() enabled} and does nothing.
 */
public final class IdeSubLoggerNone extends AbstractIdeSubLogger {

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  public IdeSubLoggerNone(IdeLogLevel level) {

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

}
