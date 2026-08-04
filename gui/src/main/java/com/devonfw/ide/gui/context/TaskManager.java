package com.devonfw.ide.gui.context;

import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.progress.ProgressBarTask;

/**
 * Singleton class that manages all currently running tasks and their progress bars. It provides an {@link ObservableList} of tasks, which can be observed by
 * components like in the UI.
 *
 * @see ProgressBarTask
 */
public class TaskManager {

  private final Logger LOG = LoggerFactory.getLogger(TaskManager.class);

  private final ObservableList<ProgressBarTask> tasks = FXCollections.observableArrayList();
  private final ObservableList<ProgressBarTask> taskListReadOnly = FXCollections.unmodifiableObservableList(tasks);

  /**
   * Adds a task to the task list. The duplicate check and the add are performed atomically on the FX thread. Duplicate IDs are silently ignored (idempotent).
   *
   * @param task the task to be added to the list of tasks.
   */
  public void addTask(ProgressBarTask task) {
    assert task != null;

    // Both the duplicate check and the add happen atomically on the FX thread to avoid race conditions.
    FxHelper.runFxSafe(() -> {
      if (tasks.stream().anyMatch(t -> Objects.equals(t.getTaskId(), task.getTaskId()))) {
        LOG.error("Task with ID {} already exists.", task.getTaskId());
        return;
      }
      tasks.add(task);
    });
  }

  /**
   * Removes a task from the list.
   *
   * @param task the task to be removed.
   */
  public void removeTask(ProgressBarTask task) {
    assert task != null;

    FxHelper.runFxSafe(() -> tasks.remove(task));
  }

  /**
   * Clears the task list.
   */
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
