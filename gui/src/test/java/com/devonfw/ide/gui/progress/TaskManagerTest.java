package com.devonfw.ide.gui.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.context.TaskManager;

/**
 * Tests for the {@link TaskManager} class. We extend HeadlessApplicationTest because all TaskManager mutations run on the JavaFX Application Thread via
 * {@code FxHelper.runFxSafe()}.
 *
 * @see TaskManager
 */
class TaskManagerTest extends HeadlessApplicationTest {

  private TaskManager taskManager;

  @BeforeEach
  void setUp() {

    taskManager = new TaskManager();

    waitForFxEvents();
  }

  @Test
  void shouldAddTask() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task");

    taskManager.addTask(task);
    waitForFxEvents();

    assertEquals(1, taskManager.getTasks().size());
    assertTrue(taskManager.getTasks().contains(task));
  }

  @Test
  void shouldRemoveTask() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task");

    taskManager.addTask(task);
    waitForFxEvents();

    taskManager.removeTask(task);
    waitForFxEvents();

    assertTrue(taskManager.getTasks().isEmpty());
  }

  @Test
  void shouldIgnoreDuplicateTaskId() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task 1");
    ProgressBarTask task2 = new ProgressBarTask(taskManager, "task-1", "Test Task 2");

    taskManager.addTask(task1);
    waitForFxEvents();

    // Duplicate task IDs are silently ignored — the operation is idempotent.
    taskManager.addTask(task2);
    waitForFxEvents();

    assertEquals(1, taskManager.getTasks().size());
    assertTrue(taskManager.getTasks().contains(task1));
  }
}
