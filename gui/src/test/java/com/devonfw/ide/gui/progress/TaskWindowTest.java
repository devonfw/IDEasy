package com.devonfw.ide.gui.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.App;
import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindowController;

public class TaskWindowTest extends HeadlessApplicationTest {

  public ListView<ProgressBarTask> taskList;
  private static TaskManager taskManager;
  private Parent root;

  @Override
  public void start(Stage stage) throws Exception {

    URL taskOverviewWindowUrl = App.class.getResource("task_overview_window.fxml");
    assertThat(taskOverviewWindowUrl).as("Cannot resolve task overview window FXML resource!").isNotNull();

    FXMLLoader fxmlLoader = new FXMLLoader(taskOverviewWindowUrl);
    fxmlLoader.setController(new TaskOverviewWindowController());
    root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.show();

    taskList = (ListView<ProgressBarTask>) root.lookup("#taskList");
  }

  @BeforeAll
  public static void setup() {
    taskManager = TaskManager.getInstance();
  }

  @BeforeEach
  public void reset() {
    taskManager.getTasks().clear();
    waitForFxEvents();
  }

  /**
   * We check, whether our implementation of {@link TaskOverviewWindow#show()} actually displays the window.
   */
  @Test
  public void isWindowShown() {
    FxHelper.runFxSafe(() -> {
      TaskOverviewWindow testWindow = TaskOverviewWindow.getInstance();
      testWindow.show();

      assertThat(testWindow.getStage().isShowing()).isTrue();
    });
  }

  @Test
  public void shouldShowTaskWhenTaskAdded() {

    ProgressBarTask task = new ProgressBarTask("task-1", "Test Task");
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(taskList.getItems()).contains(task);
  }

  @Test
  public void shouldNotShowTaskWhenTaskRemoved() {

    ProgressBarTask task = new ProgressBarTask("task-1", "Test Task");
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(taskList.getItems()).contains(task);

    taskManager.removeTask(task);
    waitForFxEvents();

    assertThat(taskList.getItems()).isEmpty();
  }

  @Test
  public void listContentsAreCoherent() {

    ProgressBarTask task1 = new ProgressBarTask("task-1", "Test Task");
    ProgressBarTask task2 = new ProgressBarTask("task-2", "Test Task 2");
    ProgressBarTask task3 = new ProgressBarTask("task-3", "Test Task 3");

    taskManager.addTask(task1);
    taskManager.addTask(task2);
    taskManager.addTask(task3);
    waitForFxEvents();

    assertThat(taskList.getItems()).containsExactly(task1, task2, task3);
  }

  /**
   * Our TaskOverViewWindow should reuse the same window instance if it is already open. Also, the existing window should be brought to the front
   */
  @Test
  public void reusesExistingWindow() {

    FxHelper.runFxSafe(() -> {

      TaskOverviewWindow testWindow1 = TaskOverviewWindow.getInstance();
      testWindow1.show();

      TaskOverviewWindow testWindow2 = TaskOverviewWindow.getInstance();
      testWindow2.show();

      assertThat(testWindow1.equals(testWindow2)).isTrue().as("Window instances differentiate");
      assertThat(testWindow1.getStage().isShowing()).isTrue().as("Window is not showing");
      assertThat(testWindow1.getStage().isFocused()).isTrue().as("Window is not focused");
    });
  }

  /**
   * When no tasks are running, the TaskOverViewWindow should show an empty list, not a null pointer exception or similar.
   */
  @Test
  public void showsEmptyListWhenNoTasks() {

    // In @BeforeEach tasks get cleared before each test
    assertThat(taskList.getItems()).isEmpty();
  }

  /**
   * tests whether progress tasks are properly updated in the list when the properties update
   */
  @Test
  public void testTaskProgressUpdatesProperly() {

    ProgressBarTask task = new ProgressBarTask("task-1", "Test Task", 100, "Units", 1);
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(taskList.getItems()).as("Task should be in the list").contains(task);

    //Test stepBy (includes doStepBy; see implementation of stepBy())
    task.stepBy(20);
    waitForFxEvents();

    assertThat(taskList.getItems().getFirst().currentProgressProperty().getValue()).as("Progress should be 20").isEqualTo(20);

    //Test doStepTo (only used internally)
    task.doStepTo(40);
    waitForFxEvents();

    assertThat(taskList.getItems().getFirst().currentProgressProperty().getValue()).as("Progress should be 40").isEqualTo(40);
  }

  /**
   * We check here that a null node reference is handled properly and leads to the window being displayed in the center of the screen.
   */
  @Test
  public void testNullReferenceNode() {

    FxHelper.runFxSafe(() -> {
      TaskOverviewWindow nullRefWindow = TaskOverviewWindow.getInstance();
      nullRefWindow.showRelativeToReferenceNode(null);

      Rectangle2D screenMeasures = Screen.getPrimary().getVisualBounds();

      double expectedPositionX = screenMeasures.getWidth() / 2 - nullRefWindow.getStage().getScene().getWidth() / 2;
      double expectedPositionY = screenMeasures.getHeight() / 2 - nullRefWindow.getStage().getScene().getHeight() / 2;

      assertThat(nullRefWindow.getStage().getX()).as("Window should be in the expected X position").isEqualTo(expectedPositionX);
      assertThat(nullRefWindow.getStage().getY()).as("Window should be in the expected Y position").isEqualTo(expectedPositionY);
    });
  }
}
