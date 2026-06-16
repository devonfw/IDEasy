package com.devonfw.ide.gui.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.context.TaskManager;

/**
 * Test for the @TaskManager class. Currently, we extend HeadlessApplicationTest because we need the JavaFX Application Thread. The reason for this is, that
 * TaskManager uses a method from JavaFX (currently; looking for a better implementation in the future).
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
    taskManager.removeTask(task);

    waitForFxEvents();

    assertTrue(taskManager.getTasks().isEmpty());
  }

  @Test
  void shouldThrowExceptionWhenAddingDuplicateTaskId() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task 1");
    ProgressBarTask task2 = new ProgressBarTask(taskManager, "task-1", "Test Task 2");

    taskManager.addTask(task1);

    waitForFxEvents();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> taskManager.addTask(task2)
    );
    assertEquals("Task with ID task-1 already exists.", exception.getMessage());
  }
}
