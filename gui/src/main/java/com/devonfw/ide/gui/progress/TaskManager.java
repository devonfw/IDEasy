package com.devonfw.ide.gui.progress;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.application.Platform;

public class TaskManager {

  private static final TaskManager INSTANCE = new TaskManager();

  private final List<GuiProgressBarHandling> tasks = new CopyOnWriteArrayList<>();

  private final List<ProgressListener> listeners = new CopyOnWriteArrayList<>();

  public static TaskManager getInstance() {

    return INSTANCE;
  }

  public void addListener(ProgressListener listener) {

    listeners.add(listener);
  }

  public void removeListener(ProgressListener listener) {

    listeners.remove(listener);
  }

  /**
   * @param task the task to be added to the list of tasks.
   * @return the TaskManagers internal task ID.
   */
  public void addTask(GuiProgressBarHandling task) {

    tasks.stream()
        .filter(t ->
            t.getTaskId() == task.getTaskId()
        )
        .findFirst()
        .ifPresent(existingTask -> {
          throw new IllegalArgumentException("Task with ID " + task.getTaskId() + " already exists.");
        });
    tasks.add(task);
    Platform.runLater(() ->
        listeners.forEach(listener -> listener.onProgressTaskAdded(tasks))
    );
  }

  /**
   * @param taskId the ID of the task to be removed from the list of tasks.
   */
  public void removeTask(String taskId) {

    tasks.stream().filter(task -> task.getTaskId() == taskId).findFirst().ifPresent(tasks::remove);
    Platform.runLater(() ->
        listeners.forEach(listener -> listener.onProgressTaskRemoved(tasks))
    );
  }

  public void removeTask(GuiProgressBarHandling task) {

    tasks.remove(task);
    Platform.runLater(() ->
        listeners.forEach(listener -> listener.onProgressTaskRemoved(tasks))
    );
  }

  /**
   * @return the list of currently running tasks.
   */
  public List<GuiProgressBarHandling> getTasks() {

    return tasks;
  }

  protected void updateTask(GuiProgressBarHandling task, long stepPosition) {

    Platform.runLater(() ->
        listeners.forEach(listener -> listener.onProgressTaskUpdated(task, stepPosition))
    );
  }

  /**
   * Listener interface for receiving updates about progress tasks. Implement this interface and register it with the TaskManager to receive updates when tasks
   * are added, removed, or updated.
   */
  public interface ProgressListener {

    /**
     * @param task updated task
     * @param stepPosition the current position of the task (for STEP updates)
     */
    void onProgressTaskUpdated(GuiProgressBarHandling task, long stepPosition);

    /**
     * @param updatedTaskList the updated list of tasks after a task was added
     */
    void onProgressTaskAdded(List<GuiProgressBarHandling> updatedTaskList);

    /**
     * @param updatedTaskList the updated list of tasks after a task was removed
     */
    void onProgressTaskRemoved(List<GuiProgressBarHandling> updatedTaskList);
  }
}
