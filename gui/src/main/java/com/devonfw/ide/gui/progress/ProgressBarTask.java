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
import com.devonfw.tools.ide.io.AbstractIdeProgressBar;

/**
 * This is a handler for the progress bars in the GUI
 */
public class ProgressBarTask extends AbstractIdeProgressBar {

  private static final Logger LOG = LoggerFactory.getLogger(ProgressBarTask.class.getName());

  private boolean isIndeterminate = false;
  private final String taskId;

  private final LongProperty progressProperty = new SimpleLongProperty(getCurrentProgress());
  //we only set the title on this line, once. title is final in AbstractIdeContext, but we also use a property here, to be consistent and allow dynamic updates if needed in the future.
  private final StringProperty titleProperty = new SimpleStringProperty(getTitle());
  private final BooleanProperty indeterminateProperty = new SimpleBooleanProperty(isIndeterminate());

  /**
   * @param taskId a unique identifier for this task, used for example to prevent duplicate tasks in the TaskManager
   * @param title title of the task
   * @param maxSize maximum progress
   * @param unitName unit of the progress (e.g. %, MB, files, etc.)
   * @param unitSize unit size (e.g. 1%)
   */
  public ProgressBarTask(String taskId, String title, long maxSize, String unitName, long unitSize) {

    super(title, maxSize, unitName, unitSize);
    this.taskId = taskId;
    TaskManager.getInstance().addTask(this);
  }

  /**
   * This constructor is used for indeterminate progress bars in the UI.
   *
   * @param title the title of the progress bar
   */
  public ProgressBarTask(String taskId, String title) {

    super(title, 100, "%", 1);
    setIndeterminate(true);
    this.taskId = taskId;
    TaskManager.getInstance().addTask(this);
  }

  @Override
  protected void doStepBy(long stepSize, long currentProgress) {

    LOG.debug("Updating progress bar to {}", currentProgress);

    FxHelper.runFxSafe(() -> progressProperty.setValue(currentProgress));
  }

  @Override
  protected void doStepTo(long stepPosition) {

    LOG.debug("Updating progress bar to {}", getCurrentProgress());

    FxHelper.runFxSafe(() -> progressProperty.setValue(getCurrentProgress()));
  }

  @Override
  public long getCurrentProgress() {

    return super.getCurrentProgress();
  }

  @Override
  public void close() {

    LOG.info("Closing progress bar");
    TaskManager.getInstance().removeTask(this);
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
