package com.devonfw.tools.ide.context;

import java.util.Scanner;

import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLoggerOut;

import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

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

    super(minLogLevel, level -> new IdeSubLoggerOut(level, out, colored, minLogLevel), null, null);
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

  @Override
  public IdeProgressBar prepareProgressBar(String taskName, long size) {

    ProgressBarBuilder pbb = new ProgressBarBuilder();
    // default (COLORFUL_UNICODE_BLOCK)
    pbb.setStyle(ProgressBarStyle.builder().refreshPrompt("\r").leftBracket("\u001b[33m│").delimitingSequence("")
        .rightBracket("│\u001b[0m").block('█').space(' ').fractionSymbols(" ▏▎▍▌▋▊▉").rightSideFractionSymbol(' ')
        .build());
    // set different style for Windows systems (ASCII)
    if (getSystemInfo().isWindows()) {
      pbb.setStyle(ProgressBarStyle.builder().refreshPrompt("\r").leftBracket("[").delimitingSequence("")
          .rightBracket("]").block('=').space(' ').fractionSymbols(">").rightSideFractionSymbol(' ').build());
    }
    pbb.showSpeed();
    pbb.setTaskName(taskName);
    pbb.setUnit("MiB", 1048576);
    pbb.setInitialMax(size);
    pbb.continuousUpdate();
    pbb.setUpdateIntervalMillis(1);
    return new IdeProgressBarConsole(pbb.build());
  }

}
