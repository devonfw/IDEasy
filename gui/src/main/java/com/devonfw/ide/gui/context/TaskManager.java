package com.devonfw.ide.gui.context;

import java.util.Objects;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.progress.GuiTask;

/**
 * Manages all tasks currently shown to the end-user and provides them as an {@link ObservableList} that UI components can observe.
 * <p>
 * Both progress bars and steps are held in this single list as {@link GuiTask}s, so that the status bar and the task overview window need only one rendering
 * path. The list is created with an extractor, so a change to a task's own properties also notifies list observers.
 *
 * @see com.devonfw.ide.gui.progress.ProgressBarTask
 * @see com.devonfw.ide.gui.progress.step.GuiStep
 */
public class TaskManager {

  private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);

  private final ObservableList<GuiTask> tasks = FXCollections.observableArrayList(
      task -> new Observable[] { task.progressProperty(), task.detailProperty(), task.stateProperty() });

  private final ObservableList<GuiTask> taskListReadOnly = FXCollections.unmodifiableObservableList(this.tasks);

  /**
   * Adds a task to the task list. The duplicate check and the add are performed atomically on the FX thread. Duplicate IDs are silently ignored (idempotent).
   *
   * @param task the task to be added to the list of tasks.
   */
  public void addTask(GuiTask task) {

    Objects.requireNonNull(task, "task");

    // Both the duplicate check and the add happen atomically on the FX thread to avoid race conditions.
    FxHelper.runFxSafe(() -> {
      if (this.tasks.stream().anyMatch(t -> Objects.equals(t.getId(), task.getId()))) {
        LOG.error("Task with ID {} already exists.", task.getId());
        return;
      }
      this.tasks.add(task);
    });
  }

  /**
   * Removes a task from the list.
   *
   * @param task the task to be removed.
   */
  public void removeTask(GuiTask task) {

    Objects.requireNonNull(task, "task");

    FxHelper.runFxSafe(() -> this.tasks.remove(task));
  }

  /**
   * Clears the task list.
   */
  public void clearTasks() {

    FxHelper.runFxSafe(this.tasks::clear);
  }

  /**
   * @return the {@link ObservableList} of tasks (read-only). Contains both running tasks and finished ones that the user has not dismissed yet.
   */
  public ObservableList<GuiTask> getTasks() {

    return this.taskListReadOnly;
  }
}
