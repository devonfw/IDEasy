package com.devonfw.tools.ide.context;

import java.util.Scanner;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLoggerOut;

/**
 * Default implementation of {@link IdeContext} using the console.
 */
public class IdeContextConsole extends AbstractIdeContext {

  private final Scanner scanner;

  /**
   * The constructor.
   *
   * @param minLogLevel the minimum {@link IdeLogLevel} to enable. Should be {@link IdeLogLevel#INFO} by default.
   * @param out the {@link Appendable} to {@link Appendable#append(CharSequence) write} log messages to.
   * @param colored - {@code true} for colored output according to {@link IdeLogLevel}, {@code false} otherwise.
   */
  public IdeContextConsole(IdeLogLevel minLogLevel, Appendable out, boolean colored) {

    super(minLogLevel, level -> new IdeSubLoggerOut(level, out, colored), null);
    if (System.console() == null) {
      debug("System console not available - using System.in as fallback");
      this.scanner = new Scanner(System.in);
    } else {
      this.scanner = null;
    }
  }

  @Override
  protected String readLine() {

    if (this.scanner == null) {
      return System.console().readLine();
    } else {
      return this.scanner.nextLine();
    }
  }

}
