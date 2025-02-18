package com.devonfw.tools.ide.io;

/**
 * A console-based progress bar with minimal runtime overhead.
 */
public interface IdeProgressBar extends AutoCloseable {

  /** The {@link #getTitle() title} for extracting. */
  String TITLE_EXTRACTING = "Extracting";

  /** The {@link #getTitle() title} for downloading. */
  String TITLE_DOWNLOADING = "Downloading";

  /** The {@link #getTitle() title} for copying. */
  String TITLE_COPYING = "Copying";

  /** {@link #getUnitName() Unit name} for Megabytes. */
  String UNIT_NAME_MB = "MiB";
  
  /** {@link #getUnitSize() Unit size} for Megabytes. */
  int UNIT_SIZE_MB = 1048576;

  /**
   * @return the title (task name or activity) to display in the progress bar.
   */
  String getTitle();

  /**
   * @return the maximum value when the progress bar has reached its end (100%) or {@code -1} if the maximum is undefined.
   */
  long getMaxSize();

  /**
   * @return the name of the unit displayed to the end user (e.g. "files" or "MiB").
   */
  String getUnitName();

  /**
   * @return the size of a single unit (e.g. 1 if the {@link #stepBy(long) reported progress} and {@link #getMaxSize() max size} numbers remain unchanged or
   *     1000 for "kilo" or 1000000 for "mega" in order to avoid displaying too long numbers).
   */
  long getUnitSize();

  /**
   * Increases the progress bar by given step size.
   *
   * @param stepSize size to step by.
   */
  void stepBy(long stepSize);

  /**
   * @return the total count accumulated with {@link #stepBy(long)} or {@link #getMaxSize()} in case of overflow.
   */
  long getCurrentProgress();

  @Override
  void close();
}
