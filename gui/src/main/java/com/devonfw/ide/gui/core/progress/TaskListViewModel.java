package com.devonfw.ide.gui.core.progress;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.event.TaskListChangeEvent;
import com.devonfw.ide.gui.core.event.TaskProgressEvent;
import com.devonfw.ide.gui.core.helper.CoalescingFxRunner;

import io.github.mmm.event.EventBus;

/**
 * View model mirroring the running tasks of the {@link TaskManager} onto the JavaFX thread. It is the single source for all UI components displaying tasks.
 * <p>
 * It listens to the {@link TaskListChangeEvent} and {@link TaskProgressEvent} and coalesces them into at most one pending refresh on the JavaFX thread. A
 * refresh pulls the current state from the {@link TaskManager}, so the order in which events arrive does not matter. The list is updated item by item: existing
 * {@link TaskViewModel}s are kept and updated in place (so e.g. a selection survives), new tasks are added and finished ones removed. As the list is created
 * with an extractor, a change of the properties of an item is reported as an update of the list.
 */
public class TaskListViewModel {

  private final TaskManager taskManager;

  /** Only accessed on the JavaFX thread (after construction). */
  private final ObservableList<TaskViewModel> tasks = FXCollections.observableArrayList(
      task -> new Observable[] { task.descriptionProperty(), task.progressProperty() });

  private final ObservableList<TaskViewModel> tasksReadOnly = FXCollections.unmodifiableObservableList(this.tasks);

  private final CoalescingFxRunner refresh = new CoalescingFxRunner(this::refreshTasks);

  /**
   * @param taskManager the {@link TaskManager} to mirror.
   * @param eventBus the {@link EventBus} the {@link TaskManager} sends its events to.
   */
  public TaskListViewModel(TaskManager taskManager, EventBus eventBus) {

    this.taskManager = Objects.requireNonNull(taskManager);
    // the list is not observed yet, so it is safe to fill it on the current thread (before any refresh can run on the JavaFX thread)
    List<ProgressBarTask> initialTasks = refreshTasks();
    eventBus.addListener(TaskListChangeEvent.class, _ -> this.refresh.request());
    eventBus.addListener(TaskProgressEvent.class, _ -> this.refresh.request());
    // catch up with tasks added or removed between the initial fill and the registration of the listeners (only then, as scheduling requires a JavaFX toolkit)
    if (!sameTasks(initialTasks, taskManager.getTasks())) {
      this.refresh.request();
    }
  }

  /**
   * @return the read-only {@link ObservableList} of the currently running tasks in the order they were added. Only to be accessed on the JavaFX thread.
   */
  public ObservableList<TaskViewModel> getTasks() {

    return this.tasksReadOnly;
  }

  private static boolean sameTasks(List<ProgressBarTask> tasks1, List<ProgressBarTask> tasks2) {

    if (tasks1.size() != tasks2.size()) {
      return false;
    }
    for (int i = 0; i < tasks1.size(); i++) {
      if (tasks1.get(i) != tasks2.get(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return the tasks of the {@link TaskManager} that have been applied.
   */
  private List<ProgressBarTask> refreshTasks() {

    List<ProgressBarTask> currentTasks = this.taskManager.getTasks();
    Map<ProgressBarTask, TaskViewModel> itemByTask = new IdentityHashMap<>();
    for (TaskViewModel item : this.tasks) {
      itemByTask.put(item.getTask(), item);
    }
    Set<ProgressBarTask> current = Collections.newSetFromMap(new IdentityHashMap<>());
    current.addAll(currentTasks);
    this.tasks.removeIf(item -> !current.contains(item.getTask()));
    for (int i = 0; i < currentTasks.size(); i++) {
      ProgressBarTask task = currentTasks.get(i);
      TaskViewModel item = itemByTask.get(task);
      if (item == null) {
        this.tasks.add(i, new TaskViewModel(task));
      } else {
        if (this.tasks.get(i) != item) {
          // order changed (task was removed and added again in between two refreshes)
          this.tasks.remove(item);
          this.tasks.add(i, item);
        }
        item.update();
      }
    }
    return currentTasks;
  }
}
