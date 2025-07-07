package com.devonfw.tools.ide.log;

import java.io.IOException;

/**
 * Default implementation of {@link IdeSubLogger} that can write to an {@link Appendable} such as {@link System#out} or in case of testing a
 * {@link java.io.StringWriter}.
 */
public class IdeSubLoggerOut extends AbstractIdeSubLogger {

  private final Appendable out;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   * @param out the {@link Appendable} to {@link Appendable#append(CharSequence) write} log messages to.
   * @param colored - {@code true} for colored output according to {@link IdeLogLevel}, {@code false} otherwise.
   * @param minLogLevel the minimum log level (threshold).
   * @param listener the {@link IdeLogListener} to listen to.
   */
  public IdeSubLoggerOut(IdeLogLevel level, Appendable out, boolean colored, IdeLogLevel minLogLevel, IdeLogListener listener) {

    super(level, colored, IdeLogExceptionDetails.of(level, minLogLevel), listener);
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
  }

  @Override
  public void doLog(String message, Throwable error) {

    try {
      String startColor = null;
      if (this.colored) {
        startColor = this.level.getStartColor();
        if (startColor != null) {
          this.out.append(startColor);
        }
      }
      if (error != null) {
        message = this.exceptionDetails.format(message, error);
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
}
