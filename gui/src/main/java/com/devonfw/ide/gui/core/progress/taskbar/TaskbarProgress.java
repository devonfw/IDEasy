package com.devonfw.ide.gui.core.progress.taskbar;

import javafx.stage.Stage;

import com.devonfw.tools.ide.os.SystemInfoImpl;

/**
 * Progress display in the taskbar button of a window, such as the progress bar behind the application icon in the Windows taskbar. All methods have to be
 * called on the JavaFX Application Thread.
 *
 * @see TaskbarProgressUpdater
 */
public interface TaskbarProgress {

  /** {@link TaskbarProgress} for platforms that do not support a progress in the taskbar. It simply displays nothing. */
  TaskbarProgress NONE = new TaskbarProgress() {

    @Override
    public void setProgress(double progress) {

      // not supported on this platform
    }

    @Override
    public void setIndeterminate() {

      // not supported on this platform
    }

    @Override
    public void clear() {

      // not supported on this platform
    }
  };

  /**
   * Displays a determinate progress.
   *
   * @param progress the progress to display from {@code 0.0} (nothing done yet) to {@code 1.0} (completed).
   */
  void setProgress(double progress);

  /**
   * Displays an indeterminate progress, used while work is running but its amount is unknown.
   */
  void setIndeterminate();

  /**
   * Removes the progress from the taskbar.
   */
  void clear();

  /**
   * @param stage the {@link Stage} whose taskbar button shall display the progress.
   * @return the {@link TaskbarProgress} for the given {@link Stage} on the current platform or {@link #NONE} if the platform is not supported.
   */
  static TaskbarProgress of(Stage stage) {

    if (SystemInfoImpl.INSTANCE.isWindows()) {
      return new WindowsTaskbarProgress(stage::getTitle);
    }
    return NONE;
  }
}
