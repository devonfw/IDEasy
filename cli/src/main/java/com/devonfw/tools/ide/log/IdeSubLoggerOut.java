package com.devonfw.tools.ide.log;

import java.io.IOException;

/**
 * Default implementation of {@link IdeSubLogger} that can write to an {@link Appendable} such as {@link System#out} or
 * in case of testing a {@link java.io.StringWriter}.
 */
public class IdeSubLoggerOut extends AbstractIdeSubLogger {

  private final Appendable out;

  private final boolean colored;

  private final IdeLogExceptionDetails exceptionDetails;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   * @param out the {@link Appendable} to {@link Appendable#append(CharSequence) write} log messages to.
   * @param colored - {@code true} for colored output according to {@link IdeLogLevel}, {@code false} otherwise.
   * @param minLogLevel the minimum log level (threshold).
   */
  public IdeSubLoggerOut(IdeLogLevel level, Appendable out, boolean colored, IdeLogLevel minLogLevel) {

    super(level);
    if (out == null) {
      // this is on of the very rare excuses where System.out or System.err is allowed to be used!
      if (level == IdeLogLevel.ERROR) {
        this.out = System.err;
      } else {
        this.out = System.out;
      }
    } else {
      this.out = out;
    }
    this.colored = colored;
    this.exceptionDetails = IdeLogExceptionDetails.of(level, minLogLevel);
  }

  @Override
  public boolean isEnabled() {

    return true;
  }

  @Override
  protected boolean isColored() {

    return this.colored;
  }

  @Override
  public void log(String message) {

    try {
      String startColor = null;
      if (this.colored) {
        startColor = this.level.getStartColor();
        if (startColor != null) {
          this.out.append(startColor);
        }
      }
      this.out.append(message);
      if (startColor != null) {
        this.out.append(this.level.getEndColor());
      }
      this.out.append("\n");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to log message: " + message, e);
    }
  }

  @Override
  public String log(Throwable error, String message, Object... args) {

    if (args != null) {
      message = compose(message, args);
    }
    log(this.exceptionDetails.format(message, error));
    if (message == null) {
      if (error == null) {
        return null;
      } else {
        return error.toString();
      }
    } else {
      return message;
    }
  }

}
