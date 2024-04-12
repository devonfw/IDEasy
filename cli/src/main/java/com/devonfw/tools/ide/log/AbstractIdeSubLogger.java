package com.devonfw.tools.ide.log;

/**
 * Abstract base implementation of {@link IdeSubLogger}.
 */
public abstract class AbstractIdeSubLogger implements IdeSubLogger {

  /** @see #getLevel() */
  protected final IdeLogLevel level;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  public AbstractIdeSubLogger(IdeLogLevel level) {

    super();
    this.level = level;
  }

  @Override
  public IdeLogLevel getLevel() {

    return this.level;
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
  protected boolean isColored() {

    return false;
  }

  @Override
  public String toString() {

    return getClass().getSimpleName() + "@" + this.level;
  }

}
