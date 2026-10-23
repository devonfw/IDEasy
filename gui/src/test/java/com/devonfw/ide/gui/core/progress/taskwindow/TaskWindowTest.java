package com.devonfw.ide.gui.core.progress.taskwindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.progress.ProgressBarTask;
import com.devonfw.ide.gui.core.progress.TaskListViewModel;
import com.devonfw.ide.gui.core.progress.TaskViewModel;

/**
 * Tests for the TaskOverviewWindow. We check whether the window is displayed correctly and whether it properly reacts to changes in the TaskManager.
 */
public class TaskWindowTest extends UIBasedApplicationTest {

  private ListView<TaskViewModel> taskList;

  private TaskManager taskManager;

  private TaskListViewModel taskListViewModel;

  @Override
  public void start(Stage stage) throws Exception {

    // recreated for every test, so that each test starts with an empty task list
    GuiEventBus eventBus = new GuiEventBus();
    taskManager = new TaskManager(eventBus);
    taskListViewModel = new TaskListViewModel(taskManager, eventBus);

    URL taskOverviewWindowUrl = TaskOverviewWindowController.class.getResource("task_overview_window.fxml");
    assertThat(taskOverviewWindowUrl).as("Cannot resolve task overview window FXML resource!").isNotNull();

    FXMLLoader fxmlLoader = new FXMLLoader(taskOverviewWindowUrl);
    fxmlLoader.setController(new TaskOverviewWindowController(taskListViewModel));
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.show();

    taskList = (ListView<TaskViewModel>) root.lookup("#taskList");
  }

  /**
   * Adds a task to the manager and waits for the FX thread to process it.
   */
  private void addTask(ProgressBarTask task) {
    taskManager.addTask(task);
    waitForFxEvents();
  }

  /**
   * We check, whether our implementation of {@link TaskOverviewWindow#show()} actually displays the window.
   */
  @Test
  void isWindowShown() {
    //Why use interact here instead of e.g. FxHelper.runFxSafe? -> runFxSafe executes the code asynchronously on the UI thread without waiting for the result,
    //therefore swallowing any errors. This assertion never fails if we use runFxSafe
    interact(() -> {
      TaskOverviewWindow testWindow = TaskOverviewWindow.getInstance(taskListViewModel);
      testWindow.show();

      assertThat(testWindow.getStage().isShowing()).isTrue();
    });
  }

  @Test
  void shouldShowTaskWhenTaskAdded() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task");
    addTask(task);

    assertThat(taskList.getItems()).extracting(TaskViewModel::getTask).contains(task);
  }

  @Test
  void shouldNotShowTaskWhenTaskRemoved() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task");
    addTask(task);

    assertThat(taskList.getItems()).extracting(TaskViewModel::getTask).contains(task);

    taskManager.removeTask(task);
    waitForFxEvents();

    assertThat(taskList.getItems()).isEmpty();
  }

  /**
   * Ensures that the list of tasks stays coherent with the actual tasks.
   */
  @Test
  void listContentsAreCoherent() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task");
    ProgressBarTask task2 = new ProgressBarTask(taskManager, "task-2", "Test Task 2");
    ProgressBarTask task3 = new ProgressBarTask(taskManager, "task-3", "Test Task 3");

    addTask(task1);
    addTask(task2);
    addTask(task3);

    assertThat(taskList.getItems()).extracting(TaskViewModel::getTask).containsExactly(task1, task2, task3);
  }

  /**
   * Our TaskOverViewWindow should reuse the same window instance if it is already open. Also, the existing window should be brought to the front
   */
  @Test
  void reusesExistingWindow() {

    interact(() -> {

      TaskOverviewWindow testWindow1 = TaskOverviewWindow.getInstance(taskListViewModel);
      testWindow1.show();

      TaskOverviewWindow testWindow2 = TaskOverviewWindow.getInstance(taskListViewModel);
      testWindow2.show();

      assertThat(testWindow1.equals(testWindow2)).as("Window instances differentiate").isTrue();
      assertThat(testWindow1.getStage().isShowing()).as("Window is not showing").isTrue();
      assertThat(testWindow1.getStage().isFocused()).as("Window is not focused").isTrue();
    });
  }

  /**
   * When no tasks are running, the TaskOverViewWindow should show an empty list, not a null pointer exception or similar.
   */
  @Test
  void showsEmptyListWhenNoTasks() {

    // start() creates a new TaskManager for each test
    assertThat(taskList.getItems()).isEmpty();
  }

  /**
   * tests whether the cells of the list are properly updated when the progress of a task changes
   */
  @Test
  void testTaskProgressUpdatesProperly() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task", 100, "Units", 1);
    addTask(task);

    assertThat(taskList.getItems()).extracting(TaskViewModel::getTask).as("Task should be in the list").contains(task);

    //Test stepBy (includes doStepBy; see implementation of stepBy())
    task.stepBy(20);
    waitForFxEvents();

    assertThat(titleLabelOf(task).getText()).isEqualTo("Test Task [20/100 Units]");
    assertThat(progressBarOf(task).getProgress()).as("Progress should be 20%").isEqualTo(0.2);

    //Test stepTo (includes doStepTo)
    task.stepTo(40);
    waitForFxEvents();

    assertThat(titleLabelOf(task).getText()).isEqualTo("Test Task [40/100 Units]");
    assertThat(progressBarOf(task).getProgress()).as("Progress should be 40%").isEqualTo(0.4);
  }

  /**
   * The items are updated in place, so a selection survives changes of the task list and of the progress.
   */
  @Test
  void testSelectionSurvivesChanges() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task 1", 100, "Units", 1);
    taskManager.addTask(task1);
    waitForFxEvents();
    interact(() -> taskList.getSelectionModel().select(0));

    taskManager.addTask(new ProgressBarTask(taskManager, "task-2", "Test Task 2"));
    task1.stepBy(10);
    waitForFxEvents();

    assertThat(taskList.getSelectionModel().getSelectedItem().getTask()).isSameAs(task1);
  }

  /**
   * An indeterminate task shows only its title and an indeterminate progress bar.
   */
  @Test
  void testIndeterminateTaskIsShownProperly() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task");
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(titleLabelOf(task).getText()).isEqualTo("Test Task");
    assertThat(progressBarOf(task).getProgress()).isEqualTo(ProgressBar.INDETERMINATE_PROGRESS);
  }

  private Label titleLabelOf(ProgressBarTask task) {

    return (Label) cellOf(task).getGraphic().lookup(".label");
  }

  private ProgressBar progressBarOf(ProgressBarTask task) {

    return (ProgressBar) cellOf(task).getGraphic().lookup(".progress-bar");
  }

  @SuppressWarnings("unchecked")
  private ListCell<TaskViewModel> cellOf(ProgressBarTask task) {

    return taskList.lookupAll(".list-cell").stream()
        .map(node -> (ListCell<TaskViewModel>) node)
        .filter(cell -> (cell.getItem() != null) && (cell.getItem().getTask() == task))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No cell shows task " + task.getTaskId()));
  }

  /**
   * We check here that a null node reference is handled properly and leads to the window being displayed in the center of the screen.
   */
  @Test
  void testNullReferenceNode() {

    interact(() -> {
      TaskOverviewWindow nullRefWindow = TaskOverviewWindow.getInstance(taskListViewModel);
      nullRefWindow.showRelativeToReferenceNode(null);

      Rectangle2D screenMeasures = Screen.getPrimary().getVisualBounds();

      double expectedPositionX = screenMeasures.getWidth() / 2 - nullRefWindow.getStage().getWidth() / 2;
      double expectedPositionY = screenMeasures.getHeight() / 2 - nullRefWindow.getStage().getHeight() / 2;

      assertThat(nullRefWindow.getStage().getX()).as("Window should be in the expected X position").isEqualTo(expectedPositionX);
      assertThat(nullRefWindow.getStage().getY()).as("Window should be in the expected Y position").isEqualTo(expectedPositionY);
    });
  }
}
