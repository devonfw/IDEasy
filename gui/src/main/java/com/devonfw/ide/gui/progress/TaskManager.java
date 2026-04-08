package com.devonfw.ide.gui.progress;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.application.Platform;

public class TaskManager {

  private static final TaskManager INSTANCE = new TaskManager();

  public static TaskManager getInstance() {

    return INSTANCE;
  }

  public void addListener(ProgressListener listener) {

    listeners.add(listener);
  }

  public void removeListener(ProgressListener listener) {

    listeners.remove(listener);
  }

  public void addTask(GuiProgressBarHandling task) {

    tasks.add(task);
    Platform.runLater(() ->
        listeners.forEach(listener -> listener.onProgressTaskAdded(tasks))
    );
  }

  public void removeTask(GuiProgressBarHandling task) {

    tasks.remove(task);
    Platform.runLater(() ->
        listeners.forEach(listener -> listener.onProgressTaskRemoved(tasks))
    );
  }

  public List<GuiProgressBarHandling> getTasks() {

    return tasks;
  }

  //TODO: remove after testing
  public void removeLastTask() {

    tasks.removeLast();
    listeners.forEach(listener -> listener.onProgressTaskRemoved(tasks));
  }

  protected void updateTask(GuiProgressBarHandling task, long stepPosition) {

    Platform.runLater(() ->
        listeners.forEach(listener -> listener.onProgressTaskUpdate(task, stepPosition))
    );
  }

  private final List<GuiProgressBarHandling> tasks = new CopyOnWriteArrayList<>();

  private final List<ProgressListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Listener interface for receiving updates about progress tasks. Implement this interface and register it with the TaskManager to receive updates when tasks
   * are added, removed, or updated.
   */
  public interface ProgressListener {

    /**
     * @param task the task that was updated
     * @param stepPosition the current position of the task (for STEP updates)
     */
    void onProgressTaskUpdate(GuiProgressBarHandling task, long stepPosition);

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
