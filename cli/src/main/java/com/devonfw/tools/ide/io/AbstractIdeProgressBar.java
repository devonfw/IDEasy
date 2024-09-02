package com.devonfw.tools.ide.io;

/**
 * Abstract implementation of {@link IdeProgressBar}.
 */
public abstract class AbstractIdeProgressBar implements IdeProgressBar {

  private long currentProgress;
  private final long maxLength;

  /**
   * @param maxLength the maximum length of the progress bar.
   */
  public AbstractIdeProgressBar(long maxLength) {

    this.maxLength = maxLength;
  }

  /**
   * Increases the progress bar by given step size.
   *
   * @param stepSize size to step by.
   * @param currentProgress current progress state.
   */
  protected abstract void doStepBy(long stepSize, long currentProgress);

  /**
   * Increases the progress bar by given step size.
   *
   * @param stepSize size to step by.
   */
  protected abstract void doStepBy(long stepSize);

  /**
   * Sets the progress bar to given step position.
   *
   * @param stepPosition position to set to.
   */
  protected abstract void doStepTo(long stepPosition);

  @Override
  public void stepBy(long stepSize) {

    this.currentProgress += stepSize;
    if (this.maxLength > 0) {
      // check if maximum overflow
      if (this.currentProgress > this.maxLength) {
        this.currentProgress = this.maxLength;
        doStepTo(this.maxLength);
        return;
      }
    }

    doStepBy(stepSize, this.currentProgress);
  }

  @Override
  public void close() {
    if (this.maxLength < 0) {
      return;
    }

    if (this.currentProgress < this.maxLength) {
      doStepTo(this.maxLength);
    }
  }

}
