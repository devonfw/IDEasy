package com.devonfw.ide.gui.core.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.event.GuiEventBus;

/**
 * Tests for the {@link TaskListViewModel}. We extend {@link UIBasedApplicationTest}, because the view model mirrors the {@link TaskManager} onto the JavaFX
 * thread.
 */
class TaskListViewModelTest extends UIBasedApplicationTest {

  private GuiEventBus eventBus;

  private TaskManager taskManager;

  private TaskListViewModel viewModel;

  @BeforeEach
  void setUp() {

    this.eventBus = new GuiEventBus();
    this.taskManager = new TaskManager(this.eventBus);
    this.viewModel = new TaskListViewModel(this.taskManager, this.eventBus);
  }

  @Test
  void shouldShowTasksRegisteredBeforeCreation() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");
    this.taskManager.addTask(task);

    TaskListViewModel newViewModel = new TaskListViewModel(this.taskManager, this.eventBus);

    assertThat(newViewModel.getTasks()).extracting(TaskViewModel::getTask).containsExactly(task);
  }

  @Test
  void shouldReflectAddedAndRemovedTasksInOrder() {

    ProgressBarTask task1 = new ProgressBarTask(this.taskManager, "task-1", "Test Task 1");
    ProgressBarTask task2 = new ProgressBarTask(this.taskManager, "task-2", "Test Task 2");
    ProgressBarTask task3 = new ProgressBarTask(this.taskManager, "task-3", "Test Task 3");

    this.taskManager.addTask(task1);
    this.taskManager.addTask(task2);
    this.taskManager.addTask(task3);
    waitForFxEvents();
    assertThat(this.viewModel.getTasks()).extracting(TaskViewModel::getTask).containsExactly(task1, task2, task3);

    task2.close();
    waitForFxEvents();
    assertThat(this.viewModel.getTasks()).extracting(TaskViewModel::getTask).containsExactly(task1, task3);
  }

  /**
   * A task that is removed and added again moves to the end, just like in the {@link TaskManager}, even if the view model never saw it removed.
   */
  @Test
  void shouldFollowOrderOfTaskManager() {

    ProgressBarTask task1 = new ProgressBarTask(this.taskManager, "task-1", "Test Task 1");
    ProgressBarTask task2 = new ProgressBarTask(this.taskManager, "task-2", "Test Task 2");
    this.taskManager.addTask(task1);
    this.taskManager.addTask(task2);
    waitForFxEvents();

    interact(() -> {
      // on the FX thread, the view model cannot refresh between these calls
      this.taskManager.removeTask(task1);
      this.taskManager.addTask(task1);
    });
    waitForFxEvents();

    assertThat(this.viewModel.getTasks()).extracting(TaskViewModel::getTask).containsExactly(task2, task1);
  }

  /**
   * Items are stable: a progress change updates the existing item instead of replacing it (so e.g. a selection in a list survives).
   */
  @Test
  void shouldUpdateExistingItemOnProgress() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task", 100, "Units", 1);
    this.taskManager.addTask(task);
    waitForFxEvents();
    TaskViewModel item = this.viewModel.getTasks().getFirst();
    assertThat(item.getDescription()).isEqualTo("Test Task [0/100 Units]");
    assertThat(item.getProgress()).isEqualTo(0.0);

    task.stepBy(20);
    waitForFxEvents();

    assertThat(this.viewModel.getTasks()).containsExactly(item);
    assertThat(item.getDescription()).isEqualTo("Test Task [20/100 Units]");
    assertThat(item.getProgress()).isEqualTo(0.2);
  }

  @Test
  void shouldShowIndeterminateTaskWithTitleOnly() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");
    this.taskManager.addTask(task);
    waitForFxEvents();
    TaskViewModel item = this.viewModel.getTasks().getFirst();

    assertThat(item.getDescription()).isEqualTo("Test Task");
    assertThat(item.getProgress()).isEqualTo(TaskViewModel.INDETERMINATE_PROGRESS);

    task.setIndeterminate(false);
    waitForFxEvents();

    assertThat(item.getDescription()).isEqualTo("Test Task [0/100 %]");
    assertThat(item.getProgress()).isEqualTo(0.0);
  }

  /**
   * Many progress steps while the JavaFX thread is busy must result in a single UI update.
   */
  @Test
  void shouldCoalesceProgressUpdates() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task", 100, "Units", 1);
    this.taskManager.addTask(task);
    waitForFxEvents();
    TaskViewModel item = this.viewModel.getTasks().getFirst();
    AtomicInteger descriptionChanges = new AtomicInteger();
    item.descriptionProperty().addListener((_, _, _) -> descriptionChanges.incrementAndGet());

    // on the FX thread, the view model cannot refresh before all steps are done
    interact(() -> {
      for (int i = 0; i < 100; i++) {
        task.stepBy(1);
      }
    });
    waitForFxEvents();

    assertThat(item.getDescription()).isEqualTo("Test Task [100/100 Units]");
    assertThat(descriptionChanges).hasValue(1);
  }
}
