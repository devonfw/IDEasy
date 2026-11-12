package com.devonfw.ide.gui.core.progress;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.event.TaskProgressEvent;
import com.devonfw.tools.ide.io.AbstractIdeProgressBar;

/**
 * This is a handler for the progress bars in the GUI. It is plain Java without any JavaFX dependency: the progress is advanced by a worker thread, reported to
 * the {@link TaskManager} (that sends a {@link TaskProgressEvent}), and read by the UI on the JavaFX thread.
 */
public class ProgressBarTask extends AbstractIdeProgressBar {

  /**
   * This is the format representing how task titles are displayed in the UI. It follows the scheme "Task title [current progress/maximum progress Unit]"
   */
  public static final String TASK_DESCRIPTION_STRING_FORMAT = "%s [%d/%d %s]";

  private static final Logger LOG = LoggerFactory.getLogger(ProgressBarTask.class.getName());

  private final TaskManager taskManager;

  private final String taskId; //We use a task id to differentiate between multiple tasks with the same title.

  private volatile boolean indeterminate;

  /**
   * Mirror of the progress of {@link AbstractIdeProgressBar}, whose field is not {@code volatile} and therefore not safely readable from the JavaFX thread.
   */
  private volatile long progress;

  /**
   * @param taskManager the {@link TaskManager} to link this progress bar to. It is notified about progress changes and used for closing the task, in case
   *     {@link #close()} is called.
   * @param taskId a unique id to identify this task.
   * @param title title of the task
   * @param maxSize maximum progress
   * @param unitName unit of the progress (e.g., %, MB, files, etc.)
   * @param unitSize unit size (e.g., 1%)
   */
  public ProgressBarTask(TaskManager taskManager, String taskId, String title, long maxSize, String unitName, long unitSize) {

    super(title, maxSize, unitName, unitSize);
    this.taskManager = Objects.requireNonNull(taskManager);
    this.taskId = taskId;
  }

  /**
   * This constructor is used for indeterminate progress bars in the UI.
   *
   * @param taskManager the {@link TaskManager} to link this progress bar to. It is notified about progress changes and used for closing the task, in case
   *     {@link #close()} is called.
   * @param taskId a unique id to identify this task.
   * @param title the title of the progress bar
   */
  public ProgressBarTask(TaskManager taskManager, String taskId, String title) {

    this(taskManager, taskId, title, 100, "%", 1);
    this.indeterminate = true;
  }

  //currentProgress is only for test purposes, see AbstractIdeProgressBar
  @Override
  protected void doStepBy(long stepSize, long currentProgress) {

    LOG.debug("Updating progress bar by {} to {}", stepSize, currentProgress);
    this.progress = currentProgress;
    this.taskManager.notifyProgress(this);
  }

  /**
   * Exposed publicly (wider than the {@code protected} declaration in {@link AbstractIdeProgressBar}) so the tests can drive the progress bar to a precise
   * position.
   */
  @Override
  public void stepTo(long stepPosition) {

    super.stepTo(stepPosition);
  }

  @Override
  protected void doStepTo(long stepPosition) {

    LOG.debug("Updating progress bar to {}", stepPosition);
    this.progress = stepPosition;
    this.taskManager.notifyProgress(this);
  }

  @Override
  public long getCurrentProgress() {

    return this.progress;
  }

  @Override
  public void close() {

    LOG.info("Closing progress bar");
    this.taskManager.removeTask(this);
    super.close();
  }

  /**
   * @return true if the progress bar is indeterminate, false otherwise
   */
  public boolean isIndeterminate() {

    return this.indeterminate;
  }

  /**
   * @param indeterminate set whether the progress bar is indeterminate or not
   */
  public void setIndeterminate(boolean indeterminate) {

    this.indeterminate = indeterminate;
    this.taskManager.notifyProgress(this);
  }

  /**
   * @return id of the current task
   */
  public String getTaskId() {

    return this.taskId;
  }
}
