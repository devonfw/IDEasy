package com.devonfw.tools.ide.io;

/**
 * A console-based progress bar with minimal runtime overhead.
 */
public interface IdeProgressBar extends AutoCloseable {

  /**
   * Increases the progress bar by given step size.
   *
   * @param stepSize size to step by.
   */
  void stepBy(long stepSize);

  /**
   * @return the maximum value when the progress bar has reached its end or {@code -1} if the maximum is undefined.
   */
  long getMaxLength();

  /**
   * @return the total count accumulated with {@link #stepBy(long)} or {@link #getMaxLength()} in case of overflow.
   */
  long getCurrentProgress();

  @Override
  void close();
}
