package com.devonfw.tools.ide.log;

import java.io.IOException;

/**
 * Default implementation of {@link IdeSubLogger} that can write to an {@link Appendable} such as {@link System#out} or in
 * case of testing a {@link java.io.StringWriter}.
 */
public class IdeSubLoggerOut extends AbstractIdeSubLogger {

  private final Appendable out;

  private final boolean colored;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   * @param out the {@link Appendable} to {@link Appendable#append(CharSequence) write} log messages to.
   * @param colored - {@code true} for colored output according to {@link IdeLogLevel}, {@code false} otherwise.
   */
  public IdeSubLoggerOut(IdeLogLevel level, Appendable out, boolean colored) {

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
  }

  @Override
  public boolean isEnabled() {

    return true;
  }

  @Override
  public void log(String message) {

    try {
      if (this.colored) {
        this.out.append(this.level.getStartColor());
      }
      this.out.append(message);
      if (this.colored) {
        this.out.append(this.level.getEndColor());
      }
      this.out.append("\n");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to log message: " + message, e);
    }
  }

}
