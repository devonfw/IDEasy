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
  public void log(String message) {

  }

  @Override
  public boolean isEnabled() {

    return false;
  }

}
