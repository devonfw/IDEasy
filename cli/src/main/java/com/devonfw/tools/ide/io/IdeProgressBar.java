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
   * @return the current progressbar size
   */
  long getCurrent();

  /**
   * Increases the progress bar by one step.
   */
  default void stepByOne() {

    stepBy(1);
  }

  @Override
  void close();


}
