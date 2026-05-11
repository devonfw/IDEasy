package com.devonfw.ide.gui.progress;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TaskManager {

  private static final TaskManager INSTANCE = new TaskManager();

  private final ObservableList<ProgressBarTask> tasks = FXCollections.observableList(new CopyOnWriteArrayList<>());

  public static TaskManager getInstance() {

    return INSTANCE;
  }

  /**
   * @param task the task to be added to the list of tasks.
   * @return the TaskManagers internal task ID.
   */

  public void addTask(ProgressBarTask task) {
    boolean exists = tasks.stream()
        .anyMatch(t -> Objects.equals(t.getTaskId(), task.getTaskId()));
    if (exists) {
      throw new IllegalArgumentException("Task with ID " + task.getTaskId() + " already exists.");
    }

    tasks.add(task);
  }

  public void removeTask(ProgressBarTask task) {

    tasks.remove(task);
  }

  /**
   * @return the list of currently running tasks.
   */
  public ObservableList<ProgressBarTask> getTasks() {

    return tasks;
  }
}
