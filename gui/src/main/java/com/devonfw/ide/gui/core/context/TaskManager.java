package com.devonfw.ide.gui.core.context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.event.TaskListChangeEvent;
import com.devonfw.ide.gui.core.event.TaskProgressEvent;
import com.devonfw.ide.gui.core.progress.ProgressBarTask;

import io.github.mmm.event.EventBus;

/**
 * Manages all currently running tasks and their progress bars. This class is plain Java without any JavaFX dependency and is thread-safe: all operations take
 * effect immediately on the calling thread, so the state returned by {@link #getTasks()} is always up to date. After every effective change, a
 * {@link TaskListChangeEvent} is sent via the {@link EventBus}, and a {@link TaskProgressEvent} after the progress of a running task has changed. UI components
 * listen to these events and pull the current state (on the JavaFX thread).
 *
 * @see ProgressBarTask
 */
public class TaskManager {

  private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);

  private final EventBus eventBus;

  /** The tasks by their {@link ProgressBarTask#getTaskId() ID} in insertion order. Guarded by {@code this.tasks}. */
  private final Map<String, ProgressBarTask> tasks = new LinkedHashMap<>();

  /**
   * @param eventBus the {@link EventBus} used to send a {@link TaskListChangeEvent} after every change.
   */
  public TaskManager(EventBus eventBus) {

    this.eventBus = Objects.requireNonNull(eventBus);
  }

  /**
   * Adds a task to the task list. A task with an already registered ID is ignored (idempotent).
   *
   * @param task the task to be added to the list of tasks.
   * @return {@code true} if the task was added, {@code false} if a task with the same ID already exists.
   */
  public boolean addTask(ProgressBarTask task) {

    Objects.requireNonNull(task);
    synchronized (this.tasks) {
      if (this.tasks.putIfAbsent(task.getTaskId(), task) != null) {
        LOG.error("Task with ID {} already exists.", task.getTaskId());
        return false;
      }
    }
    fireChange();
    return true;
  }

  /**
   * Removes a task from the task list. Only the registered task instance is removed, so a rejected duplicate with the same ID has no effect.
   *
   * @param task the task to be removed.
   * @return {@code true} if the task was removed, {@code false} if it was not registered.
   */
  public boolean removeTask(ProgressBarTask task) {

    Objects.requireNonNull(task);
    synchronized (this.tasks) {
      if (!this.tasks.remove(task.getTaskId(), task)) {
        return false;
      }
    }
    fireChange();
    return true;
  }

  /**
   * Clears the task list.
   */
  public void clearTasks() {

    synchronized (this.tasks) {
      if (this.tasks.isEmpty()) {
        return;
      }
      this.tasks.clear();
    }
    fireChange();
  }

  /**
   * @return an immutable snapshot of the currently running tasks in the order they were added.
   */
  public List<ProgressBarTask> getTasks() {

    synchronized (this.tasks) {
      return List.copyOf(this.tasks.values());
    }
  }

  /**
   * @return {@code true} if no task is currently running, {@code false} otherwise.
   */
  public boolean isEmpty() {

    synchronized (this.tasks) {
      return this.tasks.isEmpty();
    }
  }

  /**
   * Sends a {@link TaskProgressEvent} for the given task, if it is still registered. Called by the {@link ProgressBarTask} itself.
   *
   * @param task the {@link ProgressBarTask} whose progress has changed.
   */
  public void notifyProgress(ProgressBarTask task) {

    Objects.requireNonNull(task);
    synchronized (this.tasks) {
      if (this.tasks.get(task.getTaskId()) != task) {
        return;
      }
    }
    this.eventBus.sendEvent(new TaskProgressEvent(task));
  }

  /**
   * Sends the {@link TaskListChangeEvent}. Must be called without holding the lock, as listeners may block or access this {@link TaskManager} from another
   * thread, and the {@link EventBus} serializes dispatching with its own lock (lock-order deadlock).
   */
  private void fireChange() {

    this.eventBus.sendEvent(new TaskListChangeEvent());
  }
}
