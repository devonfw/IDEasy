package com.devonfw.ide.gui.core.progress.taskbar;

import java.util.Collection;
import java.util.Objects;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.progress.ProgressBarTask;

/**
 * Displays the overall progress of all tasks of a {@link TaskManager} via a {@link TaskbarProgress}. The overall progress is the sum of the current progress
 * divided by the sum of the maximum progress of all running tasks with a known size. If tasks are running but none of them has a known size, an indeterminate
 * progress is displayed. Once no task is running anymore, the progress is cleared.
 * <p>
 * Has to be created on the JavaFX Application Thread.
 */
public class TaskbarProgressUpdater {

  /** Number of distinct progress values passed to the {@link TaskbarProgress}, so tiny changes (e.g. per downloaded chunk) do not cause an update. */
  private static final long PROGRESS_STEPS = 1000;

  /** Marker for {@link #displayedStep} if no task is running. */
  private static final long NO_TASK = -1;

  /** Marker for {@link #displayedStep} if tasks are running but none of them has a known size. */
  private static final long INDETERMINATE = -2;

  private final ObservableList<ProgressBarTask> tasks;

  private final TaskbarProgress taskbarProgress;

  private final ChangeListener<Number> progressListener = (_, _, _) -> update();

  private final ListChangeListener<ProgressBarTask> taskListener = this::onTasksChanged;

  /** The progress step (from {@code 0} to {@link #PROGRESS_STEPS}), {@link #NO_TASK} or {@link #INDETERMINATE} currently displayed. */
  private long displayedStep = NO_TASK;

  /**
   * The constructor. Immediately displays the progress of the tasks already running and keeps it up to date afterward.
   *
   * @param taskManager the {@link TaskManager} with the tasks whose progress to display.
   * @param taskbarProgress the {@link TaskbarProgress} to display the progress.
   */
  public TaskbarProgressUpdater(TaskManager taskManager, TaskbarProgress taskbarProgress) {

    this.tasks = taskManager.getTasks();
    this.taskbarProgress = Objects.requireNonNull(taskbarProgress);
    this.tasks.forEach(task -> task.currentProgressProperty().addListener(this.progressListener));
    this.tasks.addListener(this.taskListener);
    update();
  }

  private void onTasksChanged(ListChangeListener.Change<? extends ProgressBarTask> change) {

    while (change.next()) {
      change.getRemoved().forEach(task -> task.currentProgressProperty().removeListener(this.progressListener));
      change.getAddedSubList().forEach(task -> task.currentProgressProperty().addListener(this.progressListener));
    }
    update();
  }

  private void update() {

    long step = computeStep(this.tasks);
    if (step == this.displayedStep) {
      return;
    }
    this.displayedStep = step;
    if (step == NO_TASK) {
      this.taskbarProgress.clear();
    } else if (step == INDETERMINATE) {
      this.taskbarProgress.setIndeterminate();
    } else {
      this.taskbarProgress.setProgress((double) step / PROGRESS_STEPS);
    }
  }

  private static long computeStep(Collection<ProgressBarTask> tasks) {

    if (tasks.isEmpty()) {
      return NO_TASK;
    }
    long completed = 0;
    long total = 0;
    for (ProgressBarTask task : tasks) {
      long maxSize = task.getMaxSize();
      if (!task.isIndeterminate() && (maxSize > 0)) {
        completed += Math.clamp(task.currentProgressProperty().get(), 0, maxSize);
        total += maxSize;
      }
    }
    if (total == 0) {
      return INDETERMINATE;
    }
    return Math.round((double) completed / total * PROGRESS_STEPS);
  }
}
