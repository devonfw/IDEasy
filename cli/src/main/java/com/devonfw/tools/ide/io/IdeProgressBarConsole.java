package com.devonfw.tools.ide.io;

import me.tongfei.progressbar.ProgressBar;

/**
 * Implementation of {@link IdeProgressBar}.
 */
public class IdeProgressBarConsole implements IdeProgressBar {

  private final ProgressBar progressBar;

  /**
   * The constructor.
   *
   * @param progressBar the {@link ProgressBar} to initialize.
   */
  public IdeProgressBarConsole(ProgressBar progressBar) {

    this.progressBar = progressBar;
  }

  @Override
  public void stepBy(long stepSize) {

    this.progressBar.stepBy(stepSize);
  }

  @Override
  public void close() {

    this.progressBar.close();
  }
}
