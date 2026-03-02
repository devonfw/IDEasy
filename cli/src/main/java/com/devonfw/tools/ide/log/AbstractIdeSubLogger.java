package com.devonfw.tools.ide.log;

import com.devonfw.tools.ide.cli.CliException;

/**
 * Abstract base implementation of {@link IdeSubLogger}.
 */
public abstract class AbstractIdeSubLogger implements IdeSubLogger {

  /** @see #getLevel() */
  protected final IdeLogLevel level;

  protected final IdeLogExceptionDetails exceptionDetails;

  final IdeLogListener listener;

  protected boolean colored;

  private boolean enabled;

  private IdeLogArgFormatter argFormatter;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   * @param colored - see {@link #isColored()}.
   * @param exceptionDetails the {@link IdeLogExceptionDetails} configuring how to handle exceptions.
   * @param listener the {@link IdeLogListener} to send log-events to.
   */
  public AbstractIdeSubLogger(IdeLogLevel level, boolean colored, IdeLogExceptionDetails exceptionDetails, IdeLogListener listener) {

    super();
    this.level = level;
    this.exceptionDetails = exceptionDetails;
    this.argFormatter = IdeLogArgFormatter.DEFAULT;
    if (listener == null) {
      this.listener = IdeLogListenerNone.INSTANCE;
    } else {
      this.listener = listener;
    }
    this.colored = colored;
    this.enabled = true;
  }

  @Override
  public IdeLogLevel getLevel() {

    return this.level;
  }

  @Override
  public boolean isEnabled() {

    return this.enabled;
  }

  void setEnabled(boolean enabled) {

    this.enabled = enabled;
  }

  void setColored(boolean colored) {
    
    this.colored = colored;
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
      sb.append(this.argFormatter.formatArgument(args[argIndex++]));
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

    boolean colored = isColored();
    if (colored) {
      System.err.print(IdeLogLevel.ERROR.getEndColor());
      System.err.print(IdeLogLevel.ERROR.getStartColor());
    }
    System.err.println(message);
    if (colored) {
      System.err.print(IdeLogLevel.ERROR.getEndColor());
    }
  }

  /**
   * @return {@code true} if colored logging is used, {@code false} otherwise.
   */
  public boolean isColored() {

    return this.colored;
  }

  @Override
  public String log(Throwable error, String message, Object... args) {

    if (!this.enabled) {
      // performance optimization: do not fill in arguments if disabled
      return message;
    }
    String actualMessage = message;
    if (error != null) {
      if (isOmitStacktrace(error)) {
        if (message == null) {
          actualMessage = error.getMessage();
        }
        error = null;
      } else if (message == null) {
        actualMessage = error.toString();
      }
    }
    if (actualMessage == null) {
      actualMessage = "Internal error: Both message and error is null - nothing to log!";
      // fail fast if assertions are enabled, so developers of IDEasy will find the bug immediately but in productive use better log the error and continue
      assert false : actualMessage;
    } else if ((args != null) && (args.length > 0)) {
      actualMessage = compose(actualMessage, args);
    }
    boolean accept = this.listener.onLog(this.level, actualMessage, message, args, error);
    if (accept) {
      doLog(actualMessage, error);
    }
    return actualMessage;
  }

  private boolean isOmitStacktrace(Throwable error) {

    return (error instanceof CliException);
  }

  /**
   * @param message the formatted message to log.
   * @param error the optional {@link Throwable} to log or {@code null} for no error.
   */
  protected abstract void doLog(String message, Throwable error);

  /**
   * @param argFormatter the new {@link IdeLogArgFormatter} to use.
   */
  void setArgFormatter(IdeLogArgFormatter argFormatter) {

    this.argFormatter = argFormatter;
  }

  @Override
  public String toString() {

    return getClass().getSimpleName() + "@" + this.level;
  }

}
