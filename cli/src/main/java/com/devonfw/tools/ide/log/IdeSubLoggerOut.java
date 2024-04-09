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
  public void log(Throwable error, String message, Object... args) {

    if (args != null) {
      message = compose(message, args);
    }
    log(this.exceptionDetails.format(message, error));
  }

  /**
   * Should only be used internally by logger implementation.
   *
   * @param message the message template.
   * @param args the dynamic arguments to fill in.
   * @return the resolved message with the parameters filled in.
   */
  protected String compose(String message, Object... args) {

    int pos = message.indexOf("{}");
    if (pos < 0) {
      if (args.length > 0) {
        invalidMessage(message, false, args);
      }
      return message;
    }
    int argIndex = 0;
    int start = 0;
    int length = message.length();
    StringBuilder sb = new StringBuilder(length + 48);
    while (pos >= 0) {
      sb.append(message, start, pos);
      sb.append(args[argIndex++]);
      start = pos + 2;
      pos = message.indexOf("{}", start);
      if ((argIndex >= args.length) && (pos > 0)) {
        invalidMessage(message, true, args);
        pos = -1;
      }
    }
    if (start < length) {
      String rest = message.substring(start);
      sb.append(rest);
    }
    if (argIndex < args.length) {
      invalidMessage(message, false, args);
    }
    return sb.toString();
  }

  private void invalidMessage(String message, boolean more, Object[] args) {

    warning("Invalid log message with " + args.length + " argument(s) but " + (more ? "more" : "less")
        + " placeholders: " + message);
  }

  private void warning(String message) {

    if (this.colored) {
      System.err.print(IdeLogLevel.ERROR.getEndColor());
      System.err.print(IdeLogLevel.ERROR.getStartColor());
    }
    System.err.println(message);
    if (this.colored) {
      System.err.print(IdeLogLevel.ERROR.getEndColor());
    }
  }

}
