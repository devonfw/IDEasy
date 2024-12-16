package com.devonfw.tools.ide.io;

/**
 * Abstract implementation of {@link IdeProgressBar}.
 */
public abstract class AbstractIdeProgressBar implements IdeProgressBar {

  /** @see #getTitle() */
  protected final String title;

  /** @see #getMaxSize() */
  protected final long maxSize;

  /** @see #getUnitName() */
  protected final String unitName;

  /** @see #getUnitSize() */
  protected final long unitSize;

  private long currentProgress;

  /**
   * The constructor.
   *
   * @param title the {@link #getTitle() title}.
   * @param maxSize the {@link #getMaxSize() maximum size}.
   * @param unitName the {@link #getUnitName() unit name}.
   * @param unitSize the {@link #getUnitSize() unit size}.
   */
  public AbstractIdeProgressBar(String title, long maxSize, String unitName, long unitSize) {

    super();
    this.title = title;
    this.maxSize = maxSize;
    this.unitName = unitName;
    this.unitSize = unitSize;
  }

  @Override
  public String getTitle() {

    return this.title;
  }

  @Override
  public long getMaxSize() {

    return this.maxSize;
  }

  @Override
  public String getUnitName() {

    return this.unitName;
  }

  @Override
  public long getUnitSize() {

    return this.unitSize;
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

    if ((this.maxSize > 0) && (stepPosition > this.maxSize)) {
      stepPosition = this.maxSize; // clip to max avoiding overflow
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
    if (this.maxSize > 0) {
      // check if maximum overflow
      if (this.currentProgress > this.maxSize) {
        this.currentProgress = this.maxSize;
        stepTo(this.maxSize);
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
    if (this.maxSize < 0) {
      return;
    }

    if (this.currentProgress < this.maxSize) {
      stepTo(this.maxSize);
    }
  }

}
