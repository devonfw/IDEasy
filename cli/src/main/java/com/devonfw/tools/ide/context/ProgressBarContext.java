package com.devonfw.tools.ide.context;

import me.tongfei.progressbar.ProgressBar;

public interface ProgressBarContext {

  /**
   * Prepares the {@link ProgressBar}
   *
   * @param size of the content.
   * @param taskName name of the task.
   * @return {@link ProgressBar} to use.
   */
  ProgressBar prepareProgressBar(long size, String taskName);
}
