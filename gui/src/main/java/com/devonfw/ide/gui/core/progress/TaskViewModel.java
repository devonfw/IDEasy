package com.devonfw.ide.gui.core.progress;

import java.util.Objects;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.ProgressIndicator;

/**
 * View model of a single running {@link ProgressBarTask}. It holds the JavaFX state derived from the task, so all UI components display a task the same way.
 * Only accessed on the JavaFX thread and updated by the {@link TaskListViewModel}.
 */
public class TaskViewModel {

  /** The {@link #getProgress() progress} of an {@link ProgressBarTask#isIndeterminate() indeterminate} task. */
  public static final double INDETERMINATE_PROGRESS = ProgressIndicator.INDETERMINATE_PROGRESS;

  private final ProgressBarTask task;

  private final ReadOnlyStringWrapper description = new ReadOnlyStringWrapper();

  private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();

  /**
   * @param task the {@link ProgressBarTask} to display.
   */
  TaskViewModel(ProgressBarTask task) {

    this.task = Objects.requireNonNull(task);
    update();
  }

  /**
   * Reads the current state of the {@link #getTask() task}. Properties only fire if their value actually changed.
   */
  void update() {

    if (this.task.isIndeterminate()) {
      this.description.set(this.task.getTitle());
      this.progress.set(INDETERMINATE_PROGRESS);
    } else {
      long currentProgress = this.task.getCurrentProgress();
      this.description.set(String.format(ProgressBarTask.TASK_DESCRIPTION_STRING_FORMAT, this.task.getTitle(), currentProgress, this.task.getMaxSize(),
          this.task.getUnitName()));
      this.progress.set((double) currentProgress / this.task.getMaxSize());
    }
  }

  /**
   * @return the displayed {@link ProgressBarTask}.
   */
  public ProgressBarTask getTask() {

    return this.task;
  }

  /**
   * @return the description of the task: its title, followed by the progress for determinate tasks (see
   *     {@link ProgressBarTask#TASK_DESCRIPTION_STRING_FORMAT}).
   */
  public String getDescription() {

    return this.description.get();
  }

  /**
   * @return the property of the {@link #getDescription() description}.
   */
  public ReadOnlyStringProperty descriptionProperty() {

    return this.description.getReadOnlyProperty();
  }

  /**
   * @return the progress between {@code 0} and {@code 1}, or {@link #INDETERMINATE_PROGRESS} for an indeterminate task.
   */
  public double getProgress() {

    return this.progress.get();
  }

  /**
   * @return the property of the {@link #getProgress() progress}.
   */
  public ReadOnlyDoubleProperty progressProperty() {

    return this.progress.getReadOnlyProperty();
  }
}
