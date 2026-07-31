package com.devonfw.ide.gui.context;

import java.util.ArrayList;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.progress.ProgressBarTask;

/**
 * Singleton class that manages all currently running tasks and their progress bars. It provides an {@link ObservableList} of tasks, which can be observed by
 * components like in the UI.
 *
 * @see ProgressBarTask
 */
public class TaskManager {

  private final ObservableList<ProgressBarTask> tasks = FXCollections.observableArrayList();
  private final ObservableList<ProgressBarTask> taskListReadOnly = FXCollections.unmodifiableObservableList(tasks);

  /**
   * Adds a task to the task list. Make sure to use waitForFxEvents() during testing after this method.
   *
   * @param task the task to be added to the list of tasks.
   */
  public void addTask(ProgressBarTask task) {
    assert task != null;

    // Create a defensive copy to avoid ConcurrentModificationException when the list is modified by FX listeners
    boolean exists = new ArrayList<>(tasks).stream()
        .anyMatch(t -> Objects.equals(t.getTaskId(), task.getTaskId()));
    if (exists) {
      throw new IllegalArgumentException("Task with ID " + task.getTaskId() + " already exists.");
    }

    FxHelper.runFxSafe(() -> tasks.add(task));
  }

  /**
   * Removes a task from the task list. Make sure to use waitForFxEvents() during testing after this method.
   *
   * @param task task to be removed.
   */
  public void removeTask(ProgressBarTask task) {
    assert task != null;

    FxHelper.runFxSafe(() -> tasks.remove(task));
  }

  /// clears the task list. Make sure to use waitForFxEvents() during testing after this method.
  public void clearTasks() {

    FxHelper.runFxSafe(tasks::clear);
  }

  /**
   * @return the {@link ObservableList} of currently running tasks (read-only).
   */
  public ObservableList<ProgressBarTask> getTasks() {

    return taskListReadOnly;
  }
}
