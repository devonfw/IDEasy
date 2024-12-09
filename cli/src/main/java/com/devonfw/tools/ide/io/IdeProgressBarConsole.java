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
   * @param title the title (task name or activity) to display in the progress bar.
   * @param maxSize the maximum size of the progress bar.
   * @param unitName the name of the unit to display in the progress bar.
   * @param unitSize the size of the unit (e.g. 1000 for kilo, 1000000 for mega).
   */
  public IdeProgressBarConsole(SystemInfo systemInfo, String title, long maxSize, String unitName, long unitSize) {

    super(title, maxSize, unitName, unitSize);
    this.systemInfo = systemInfo;
    this.progressBar = createProgressBar();
  }

  /**
   * @return the {@link ProgressBar}.
   */
  protected ProgressBar getProgressBar() {

    return this.progressBar;
  }

  private ProgressBar createProgressBar() {

    ProgressBarBuilder pbb = new ProgressBarBuilder();
    String leftBracket, rightBracket, fractionSymbols;
    char block;
    if (this.systemInfo.isWindows()) {
      // set different style for Windows systems (ASCII)
      leftBracket = "[";
      rightBracket = "]";
      fractionSymbols = ">";
      block = '=';
    } else {
      // default (COLORFUL_UNICODE_BLOCK)
      leftBracket = "\u001b[33m│";
      rightBracket = "│\u001b[0m";
      fractionSymbols = " ▏▎▍▌▋▊▉";
      block = '█';
    }
    pbb.setStyle(ProgressBarStyle.builder().refreshPrompt("\r").leftBracket(leftBracket).delimitingSequence("")
        .rightBracket(rightBracket).block(block).space(' ').fractionSymbols(fractionSymbols).rightSideFractionSymbol(' ')
        .build());

    pbb.setUnit(this.unitName, this.unitSize);
    if (this.maxSize <= 0) {
      pbb.setTaskName(this.title + " (unknown size)");
      pbb.setInitialMax(-1);
      pbb.hideEta();
    } else {
      pbb.setTaskName(this.title);
      pbb.showSpeed();
      pbb.setInitialMax(this.maxSize);
    }
    pbb.continuousUpdate();
    pbb.setUpdateIntervalMillis(100);

    return pbb.build();
  }

  @Override
  protected void doStepBy(long stepSize, long currentProgress) {
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
