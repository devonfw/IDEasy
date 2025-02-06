package com.devonfw.tools.ide.io;

/**
 * Implementation of {@link IdeProgressBar} that does not print anything.
 */
public final class IdeProgressBarNone extends AbstractIdeProgressBar {

  /**
   * The constructor.
   *
   * @param title the {@link #getTitle() title}.
   * @param maxSize the {@link #getMaxSize() maximum size}.
   * @param unitName the {@link #getUnitName() unit name}.
   * @param unitSize the {@link #getUnitSize() unit size}.
   */
  public IdeProgressBarNone(String title, long maxSize, String unitName, long unitSize) {

    super(title, maxSize, unitName, unitSize);
  }

  @Override
  protected void doStepBy(long stepSize, long currentProgress) {

  }

  @Override
  protected void doStepTo(long stepPosition) {

  }


}
