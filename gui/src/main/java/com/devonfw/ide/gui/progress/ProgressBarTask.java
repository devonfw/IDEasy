package com.devonfw.ide.gui.progress;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.tools.ide.io.AbstractIdeProgressBar;

/**
 * This is a handler for the progress bars in the GUI
 */
public class ProgressBarTask extends AbstractIdeProgressBar {

  private final TaskManager taskManager;
  /**
   * This is the format representing how task titles are displayed in the UI. It follows the scheme "Task title [current progress/maximum progress Unit]"
   */
  public static final String TASK_DESCRIPTION_STRING_FORMAT = "%s [%d/%d %s]";

  private static final Logger LOG = LoggerFactory.getLogger(ProgressBarTask.class.getName());

  private boolean isIndeterminate = false;
  private final String taskId; //We use a task id to differentiate between multiple tasks with the same title.

  private final LongProperty progressProperty = new SimpleLongProperty(getCurrentProgress());
  //we only set the title on this line, once. the title is final in AbstractIdeContext, but we also use a property here to be consistent and allow dynamic updates if needed in the future.
  private final StringProperty titleProperty = new SimpleStringProperty(getTitle());
  private final BooleanProperty indeterminateProperty = new SimpleBooleanProperty(isIndeterminate());

  /**
   * @param taskManager the {@link TaskManager} to link this progress bar to. Note: The task manager supplied here is only used for closing the task, in
   *     case {link #close()} is called.
   * @param taskId a unique id to identify this task.
   * @param title title of the task
   * @param maxSize maximum progress
   * @param unitName unit of the progress (e.g., %, MB, files, etc.)
   * @param unitSize unit size (e.g., 1%)
   */
  public ProgressBarTask(TaskManager taskManager, String taskId, String title, long maxSize, String unitName, long unitSize) {

    super(title, maxSize, unitName, unitSize);
    this.taskId = taskId;
    this.taskManager = taskManager;
  }

  /**
   * This constructor is used for indeterminate progress bars in the UI.
   *
   * @param taskManager the {@link TaskManager} to link this progress bar to. Note: The task manager supplied here is only used for closing the task, in
   *     case {link #close()} is called.
   * @param taskId a unique id to identify this task.
   * @param title the title of the progress bar
   */
  public ProgressBarTask(TaskManager taskManager, String taskId, String title) {

    super(title, 100, "%", 1);
    setIndeterminate(true);
    this.taskId = taskId;
    this.taskManager = taskManager;
  }

  //currentProgress is only for test purposes, see AbstractIdeProgressBar
  @Override
  protected void doStepBy(long stepSize, long currentProgress) {

    LOG.debug("Updating progress bar by {} to {}", stepSize, currentProgress);

    FxHelper.runFxSafe(() -> progressProperty.setValue(getCurrentProgress()));
  }

  @Override
  protected void doStepTo(long stepPosition) {

    LOG.debug("Updating progress bar to {}", getCurrentProgress());

    FxHelper.runFxSafe(() -> progressProperty.setValue(stepPosition));
  }

  @Override
  public void close() {

    LOG.info("Closing progress bar");
    taskManager.removeTask(this);
    super.close();
  }

  /**
   * @return true if the progress bar is indeterminate, false otherwise
   */
  public boolean isIndeterminate() {
    return isIndeterminate;
  }

  /**
   * @param indeterminate set whether the progress bar is indeterminate or not
   */
  public void setIndeterminate(boolean indeterminate) {
    isIndeterminate = indeterminate;
    indeterminateProperty.set(indeterminate);
  }

  /**
   * @return id of the current task
   */
  public String getTaskId() {
    return taskId;
  }

  /**
   * Properties are relevant for dynamically updating the ui.
   *
   * @return progress property of this task.
   */
  public LongProperty currentProgressProperty() {
    return progressProperty;
  }

  /**
   * @return title property of this task.
   */
  public StringProperty titleProperty() {
    return titleProperty;
  }

  /**
   * @return indeterminate property of this task.
   */
  public BooleanProperty indeterminateProperty() {
    return indeterminateProperty;
  }
}
