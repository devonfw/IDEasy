package com.devonfw.ide.gui.context;

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

  /**
   * @param task the task to be added to the list of tasks.
   */
  public void addTask(ProgressBarTask task) {
    assert task != null;

    boolean exists = tasks.stream()
        .anyMatch(t -> Objects.equals(t.getTaskId(), task.getTaskId()));
    if (exists) {
      throw new IllegalArgumentException("Task with ID " + task.getTaskId() + " already exists.");
    }

    FxHelper.runFxSafe(() -> tasks.add(task));
  }

  /**
   * @param task task to be removed
   */
  public void removeTask(ProgressBarTask task) {
    assert task != null;

    FxHelper.runFxSafe(() -> tasks.remove(task));
  }

  /**
   * @return the {@link ObservableList} of currently running tasks.
   */
  public ObservableList<ProgressBarTask> getTasks() {

    return tasks;
  }
}
