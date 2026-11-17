package com.devonfw.ide.gui.core.progress.taskbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.progress.ProgressBarTask;

/**
 * Test of {@link TaskbarProgressUpdater}. Extends {@link UIBasedApplicationTest} because the {@link TaskManager} and the {@link ProgressBarTask}s update their
 * state on the JavaFX Application Thread.
 */
class TaskbarProgressUpdaterTest extends UIBasedApplicationTest {

  private TaskManager taskManager;

  private TaskbarProgressMock taskbarProgress;

  private TaskbarProgressUpdater updater;

  @BeforeEach
  void setUp() {

    this.taskManager = new TaskManager();
    this.taskbarProgress = new TaskbarProgressMock();
  }

  /**
   * Verifies that the taskbar shows the sum of the current progress divided by the sum of the maximum progress of all running tasks.
   */
  @Test
  void shouldShowOverallProgressOfAllRunningTasks() {

    // arrange
    startUpdater();
    ProgressBarTask download = newTask("download", 100);
    ProgressBarTask extract = newTask("extract", 300);
    addTasks(download, extract);

    // act
    download.stepBy(50);
    extract.stepBy(150);
    waitForFxEvents();

    // assert: 50 / 400 after the download step, (50 + 150) / (100 + 300) after the extract step
    assertThat(this.taskbarProgress.calls).containsExactly("progress 0.0", "progress 0.125", "progress 0.5");
  }

  /**
   * Verifies that tasks already running when the updater is created are displayed right away.
   */
  @Test
  void shouldShowTasksAlreadyRunningOnCreation() {

    // arrange
    ProgressBarTask download = newTask("download", 100);
    addTasks(download);
    download.stepBy(40);
    waitForFxEvents();

    // act
    startUpdater();

    // assert
    assertThat(this.taskbarProgress.calls).containsExactly("progress 0.4");
  }

  /**
   * Verifies that the progress is removed from the taskbar once the last task is finished.
   */
  @Test
  void shouldClearProgressWhenLastTaskIsFinished() {

    // arrange
    startUpdater();
    ProgressBarTask download = newTask("download", 100);
    addTasks(download);

    // act
    download.close();
    waitForFxEvents();

    // assert
    assertThat(this.taskbarProgress.calls).first().isEqualTo("progress 0.0");
    assertThat(this.taskbarProgress.calls).last().isEqualTo("clear");
  }

  /**
   * Verifies that an indeterminate progress is displayed if no running task has a known size.
   */
  @Test
  void shouldShowIndeterminateProgressIfNoTaskHasKnownSize() {

    // arrange
    startUpdater();
    ProgressBarTask indeterminate = new ProgressBarTask(this.taskManager, "indeterminate", "Launching IDE");
    ProgressBarTask unknownSize = newTask("unknown-size", -1);

    // act
    addTasks(indeterminate, unknownSize);

    // assert
    assertThat(this.taskbarProgress.calls).containsExactly("indeterminate");
  }

  /**
   * Verifies that tasks without a known size are ignored as long as other tasks with a known size are running.
   */
  @Test
  void shouldIgnoreTasksWithoutKnownSizeIfOthersHaveOne() {

    // arrange
    startUpdater();
    ProgressBarTask indeterminate = new ProgressBarTask(this.taskManager, "indeterminate", "Launching IDE");
    ProgressBarTask download = newTask("download", 200);
    addTasks(indeterminate);

    // act
    addTasks(download);
    download.stepBy(50);
    waitForFxEvents();

    // assert
    assertThat(this.taskbarProgress.calls).containsExactly("indeterminate", "progress 0.0", "progress 0.25");
  }

  /**
   * Verifies that tiny progress changes (e.g. per downloaded chunk) that do not change the displayed progress do not cause an update of the taskbar.
   */
  @Test
  void shouldOnlyUpdateTaskbarIfDisplayedProgressChanges() {

    // arrange
    startUpdater();
    ProgressBarTask download = newTask("download", 1_000_000);
    addTasks(download);

    // act
    download.stepBy(100);
    download.stepBy(100);
    download.stepBy(100);
    waitForFxEvents();

    // assert
    assertThat(this.taskbarProgress.calls).containsExactly("progress 0.0");
  }

  private void startUpdater() {

    interact(() -> this.updater = new TaskbarProgressUpdater(this.taskManager, this.taskbarProgress));
    assertThat(this.updater).isNotNull();
  }

  private ProgressBarTask newTask(String taskId, long maxSize) {

    return new ProgressBarTask(this.taskManager, taskId, "Task " + taskId, maxSize, "MB", 1);
  }

  private void addTasks(ProgressBarTask... tasks) {

    for (ProgressBarTask task : tasks) {
      this.taskManager.addTask(task);
    }
    waitForFxEvents();
  }

  /**
   * {@link TaskbarProgress} recording all calls.
   */
  private static class TaskbarProgressMock implements TaskbarProgress {

    private final List<String> calls = new ArrayList<>();

    @Override
    public void setProgress(double progress) {

      this.calls.add("progress " + progress);
    }

    @Override
    public void setIndeterminate() {

      this.calls.add("indeterminate");
    }

    @Override
    public void clear() {

      this.calls.add("clear");
    }
  }
}
