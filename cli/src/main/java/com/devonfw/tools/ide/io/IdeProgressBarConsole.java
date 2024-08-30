package com.devonfw.tools.ide.io;

import com.devonfw.tools.ide.os.SystemInfo;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

/**
 * Implementation of {@link IdeProgressBar}.
 */
public class IdeProgressBarConsole extends AbstractIdeProgressBar {

  private final ProgressBar progressBar;
  private final SystemInfo systemInfo;

  /**
   * The constructor.
   *
   * @param systemInfo the {@link SystemInfo}.
   * @param taskName the {@link ProgressBar} to initialize.
   * @param maxSize the maximum size of the progress bar.
   */
  public IdeProgressBarConsole(SystemInfo systemInfo, String taskName, long maxSize) {

    super(maxSize);

    this.systemInfo = systemInfo;
    this.progressBar = createProgressBar(taskName, maxSize);
  }

  /**
   * Creates the {@link ProgressBar} initializes task name and maximum size as well as the behaviour and style.
   *
   * @param taskName name of the task.
   * @param size of the content.
   * @return {@link ProgressBar} to use.
   */
  protected ProgressBar createProgressBar(String taskName, long size) {

    ProgressBarBuilder pbb = new ProgressBarBuilder();
    // default (COLORFUL_UNICODE_BLOCK)
    pbb.setStyle(ProgressBarStyle.builder().refreshPrompt("\r").leftBracket("\u001b[33m│").delimitingSequence("")
        .rightBracket("│\u001b[0m").block('█').space(' ').fractionSymbols(" ▏▎▍▌▋▊▉").rightSideFractionSymbol(' ')
        .build());
    // set different style for Windows systems (ASCII)
    if (this.systemInfo.isWindows()) {
      pbb.setStyle(ProgressBarStyle.builder().refreshPrompt("\r").leftBracket("[").delimitingSequence("")
          .rightBracket("]").block('=').space(' ').fractionSymbols(">").rightSideFractionSymbol(' ').build());
    }

    pbb.setUnit("MiB", 1048576);
    if (size == 0) {
      pbb.setTaskName(taskName + " (unknown size)");
      pbb.setInitialMax(-1);
      pbb.hideEta();
    } else {
      pbb.setTaskName(taskName);
      pbb.showSpeed();
      pbb.setInitialMax(size);
    }
    pbb.continuousUpdate();
    pbb.setUpdateIntervalMillis(1);

    return pbb.build();
  }

  @Override
  protected void doStepBy(long stepSize, long currentProgress) {
    doStepBy(stepSize);
  }

  @Override
  protected void doStepBy(long stepSize) {
    this.progressBar.stepBy(stepSize);
  }

  @Override
  protected void doStepTo(long stepPosition) {
    this.progressBar.stepTo(stepPosition);
  }

  @Override
  public void close() {
    super.close();
    this.progressBar.close();
  }
}
