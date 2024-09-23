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

  @Override
  public long getMaxLength() {

    return maxLength;
  }

  /**
   * Increases the progress bar by given step size.
   *
   * @param stepSize size to step by.
   * @param currentProgress current progress state (used for tests only).
   */
  protected abstract void doStepBy(long stepSize, long currentProgress);

  /**
   * Sets the progress bar to given step position while making sure to avoid overflow.
   *
   * @param stepPosition position to set to.
   */
  protected void stepTo(long stepPosition) {

    if ((this.maxLength > 0) && (stepPosition > this.maxLength)) {
      stepPosition = this.maxLength; // clip to max avoiding overflow
    }
    this.currentProgress = stepPosition;
    doStepTo(stepPosition);
  }

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
        stepTo(this.maxLength);
        return;
      }
    }

    doStepBy(stepSize, this.currentProgress);
  }

  @Override
  public long getCurrentProgress() {

    return this.currentProgress;
  }

  @Override
  public void close() {
    if (this.maxLength < 0) {
      return;
    }

    if (this.currentProgress < this.maxLength) {
      stepTo(this.maxLength);
    }
  }

}
