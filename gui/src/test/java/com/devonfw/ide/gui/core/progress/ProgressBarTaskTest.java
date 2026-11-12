package com.devonfw.ide.gui.core.progress;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.event.TaskProgressEvent;

/**
 * Tests for the {@link ProgressBarTask} class. The {@link ProgressBarTask} is plain Java without any JavaFX dependency, so no JavaFX toolkit is required.
 */
class ProgressBarTaskTest extends Assertions {

  private TaskManager taskManager;

  private List<ProgressBarTask> progressEvents;

  @BeforeEach
  void setUp() {

    GuiEventBus eventBus = new GuiEventBus();
    this.taskManager = new TaskManager(eventBus);
    this.progressEvents = new CopyOnWriteArrayList<>();
    eventBus.addListener(TaskProgressEvent.class, event -> this.progressEvents.add(event.task()));
  }

  @Test
  void shouldSendProgressEventWhenSteppingRegisteredTask() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task", 100, "Units", 1);
    this.taskManager.addTask(task);

    task.stepBy(20);
    assertThat(task.getCurrentProgress()).isEqualTo(20);
    assertThat(this.progressEvents).containsExactly(task);

    task.stepTo(40);
    assertThat(task.getCurrentProgress()).isEqualTo(40);
    assertThat(this.progressEvents).containsExactly(task, task);
  }

  @Test
  void shouldNotSendProgressEventForUnregisteredTask() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task", 100, "Units", 1);

    task.stepBy(20);

    assertThat(task.getCurrentProgress()).isEqualTo(20);
    assertThat(this.progressEvents).isEmpty();
  }

  @Test
  void shouldClipProgressAtMaximum() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task", 100, "Units", 1);

    task.stepBy(150);

    assertThat(task.getCurrentProgress()).isEqualTo(100);
  }

  @Test
  void shouldRemoveTaskWithoutFurtherProgressEventWhenClosed() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task", 100, "Units", 1);
    this.taskManager.addTask(task);

    task.close();

    assertThat(this.taskManager.getTasks()).isEmpty();
    assertThat(task.getCurrentProgress()).isEqualTo(100);
    assertThat(this.progressEvents).isEmpty();
  }

  @Test
  void shouldSendProgressEventWhenIndeterminateChanges() {

    ProgressBarTask task = new ProgressBarTask(this.taskManager, "task-1", "Test Task");
    assertThat(task.isIndeterminate()).isTrue();
    this.taskManager.addTask(task);

    task.setIndeterminate(false);

    assertThat(task.isIndeterminate()).isFalse();
    assertThat(this.progressEvents).containsExactly(task);
  }
}
