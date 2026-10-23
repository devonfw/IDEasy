package com.devonfw.ide.gui.core.progress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.event.TaskListChangeEvent;

/**
 * Tests for the {@link TaskManager} class. The {@link TaskManager} is plain Java without any JavaFX dependency, so all operations take effect immediately on
 * the calling thread and no JavaFX toolkit is required.
 *
 * @see TaskManager
 */
class TaskManagerTest extends Assertions {

  private static final long TIMEOUT_SECONDS = 10;

  private GuiEventBus eventBus;

  private TaskManager taskManager;

  private AtomicInteger changeEvents;

  @BeforeEach
  void setUp() {

    this.eventBus = new GuiEventBus();
    this.taskManager = new TaskManager(this.eventBus);
    this.changeEvents = new AtomicInteger();
    this.eventBus.addListener(TaskListChangeEvent.class, event -> this.changeEvents.incrementAndGet());
  }

  @Test
  void shouldAddTaskImmediately() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");

    assertThat(this.taskManager.addTask(task)).isTrue();

    assertThat(this.taskManager.getTasks()).containsExactly(task);
    assertThat(this.taskManager.isEmpty()).isFalse();
    assertThat(this.changeEvents).hasValue(1);
  }

  @Test
  void shouldRemoveTask() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");
    this.taskManager.addTask(task);

    assertThat(this.taskManager.removeTask(task)).isTrue();

    assertThat(this.taskManager.getTasks()).isEmpty();
    assertThat(this.taskManager.isEmpty()).isTrue();
    assertThat(this.changeEvents).hasValue(2);
  }

  @Test
  void shouldRemoveTaskWhenClosed() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");
    this.taskManager.addTask(task);

    task.close();

    assertThat(this.taskManager.getTasks()).isEmpty();
  }

  @Test
  void shouldIgnoreDuplicateTaskId() {

    ProgressBarTask task1 = new ProgressBarTask(this.taskManager, "task-1", "Test Task 1");
    ProgressBarTask task2 = new ProgressBarTask(this.taskManager, "task-1", "Test Task 2");
    this.taskManager.addTask(task1);

    // Duplicate task IDs are ignored — the operation is idempotent.
    assertThat(this.taskManager.addTask(task2)).isFalse();

    assertThat(this.taskManager.getTasks()).containsExactly(task1);
    assertThat(this.changeEvents).as("a rejected add must not publish a change").hasValue(1);
  }

  /**
   * A rejected duplicate shares the task ID with the registered task. Closing it must not remove the registered task.
   */
  @Test
  void shouldNotRemoveRegisteredTaskWhenRejectedDuplicateIsRemoved() {

    ProgressBarTask task1 = new ProgressBarTask(this.taskManager, "task-1", "Test Task 1");
    ProgressBarTask duplicate = new ProgressBarTask(this.taskManager, "task-1", "Test Task 2");
    this.taskManager.addTask(task1);
    this.taskManager.addTask(duplicate);

    assertThat(this.taskManager.removeTask(duplicate)).isFalse();

    assertThat(this.taskManager.getTasks()).containsExactly(task1);
    assertThat(this.changeEvents).hasValue(1);
  }

  @Test
  void shouldNotPublishChangeWhenRemovingUnknownTask() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");

    assertThat(this.taskManager.removeTask(task)).isFalse();

    assertThat(this.changeEvents).hasValue(0);
  }

  @Test
  void shouldClearTasks() {

    this.taskManager.addTask(new ProgressBarTask(this.taskManager, "task-1", "Test Task 1"));
    this.taskManager.addTask(new ProgressBarTask(this.taskManager, "task-2", "Test Task 2"));

    this.taskManager.clearTasks();

    assertThat(this.taskManager.getTasks()).isEmpty();
    assertThat(this.changeEvents).hasValue(3);
    // clearing an empty task list is no change
    this.taskManager.clearTasks();
    assertThat(this.changeEvents).hasValue(3);
  }

  @Test
  void shouldKeepInsertionOrder() {

    ProgressBarTask task1 = new ProgressBarTask(this.taskManager, "task-b", "Test Task 1");
    ProgressBarTask task2 = new ProgressBarTask(this.taskManager, "task-a", "Test Task 2");
    ProgressBarTask task3 = new ProgressBarTask(this.taskManager, "task-c", "Test Task 3");

    this.taskManager.addTask(task1);
    this.taskManager.addTask(task2);
    this.taskManager.addTask(task3);

    assertThat(this.taskManager.getTasks()).containsExactly(task1, task2, task3);
  }

  /**
   * {@link TaskManager#getTasks()} returns an immutable snapshot that is not affected by later changes.
   */
  @Test
  void shouldReturnImmutableSnapshot() {

    ProgressBarTask task1 = new ProgressBarTask(this.taskManager, "task-1", "Test Task 1");
    this.taskManager.addTask(task1);

    List<ProgressBarTask> snapshot = this.taskManager.getTasks();
    this.taskManager.addTask(new ProgressBarTask(this.taskManager, "task-2", "Test Task 2"));

    assertThat(snapshot).containsExactly(task1);
    assertThatThrownBy(() -> snapshot.add(task1)).isInstanceOf(UnsupportedOperationException.class);
  }

  /**
   * A listener of the {@link TaskListChangeEvent} has to see the state after the change.
   */
  @Test
  void shouldPublishChangeAfterStateWasUpdated() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");
    List<List<ProgressBarTask>> seenByListener = new ArrayList<>();
    this.eventBus.addListener(TaskListChangeEvent.class, event -> seenByListener.add(this.taskManager.getTasks()));

    this.taskManager.addTask(task);
    this.taskManager.removeTask(task);

    assertThat(seenByListener).containsExactly(List.of(task), List.of());
  }

  /**
   * The change event must be published without holding the internal lock. Otherwise, a listener waiting for another thread that accesses the
   * {@link TaskManager} would deadlock.
   */
  @Test
  void shouldNotHoldLockWhilePublishingChange() throws InterruptedException {

    AtomicReference<List<ProgressBarTask>> readByOtherThread = new AtomicReference<>();
    this.eventBus.addListener(TaskListChangeEvent.class, event -> {
      Thread reader = new Thread(() -> readByOtherThread.set(this.taskManager.getTasks()), "reader");
      reader.start();
      try {
        reader.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");

    this.taskManager.addTask(task);

    assertThat(readByOtherThread.get()).as("another thread must be able to read the tasks while the change is published").containsExactly(task);
  }

  /**
   * Many threads adding and closing their tasks concurrently must leave a consistent state and publish one change per effective modification.
   */
  @Test
  void shouldHandleConcurrentModifications() throws InterruptedException {

    int threadCount = 8;
    int tasksPerThread = 500;
    List<Throwable> errors = new CopyOnWriteArrayList<>();
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threadCount);
    for (int t = 0; t < threadCount; t++) {
      String prefix = "thread-" + t + "-task-";
      Thread worker = new Thread(() -> {
        try {
          start.await();
          for (int i = 0; i < tasksPerThread; i++) {
            ProgressBarTask task = new ProgressBarTask(this.taskManager, prefix + i, "Task " + i);
            if (!this.taskManager.addTask(task)) {
              throw new IllegalStateException("Task " + task.getTaskId() + " was not added");
            }
            if (i % 2 == 0) {
              task.close();
            }
          }
        } catch (Throwable e) {
          errors.add(e);
        } finally {
          done.countDown();
        }
      });
      worker.start();
    }
    start.countDown();

    assertThat(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    assertThat(errors).isEmpty();
    assertThat(this.taskManager.getTasks()).hasSize(threadCount * tasksPerThread / 2);
    assertThat(this.changeEvents).hasValue(threadCount * tasksPerThread * 3 / 2);
  }
}
