package com.devonfw.ide.gui.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.App;
import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.progress.step.GuiStep;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindowController;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;

/**
 * Tests for the TaskOverviewWindow. We check whether the window is displayed correctly and whether it properly reacts to changes in the TaskManager.
 */
public class TaskWindowTest extends HeadlessApplicationTest {

  private TreeView<GuiTask> taskList;
  private static TaskManager taskManager;

  @TempDir
  private static Path workingDirectory;

  /**
   * @return the tasks currently displayed at the top level of the tree, i.e. below its hidden root.
   */
  private List<GuiTask> displayedTasks() {

    return taskList.getRoot().getChildren().stream().map(TreeItem::getValue).toList();
  }

  /**
   * @param task the task whose row to look up.
   * @return the {@link TreeItem} showing the given {@code task} at the top level of the tree.
   */
  private TreeItem<GuiTask> itemOf(GuiTask task) {

    return taskList.getRoot().getChildren().stream().filter(item -> item.getValue() == task).findFirst().orElseThrow();
  }

  @Override
  public void start(Stage stage) throws Exception {

    URL taskOverviewWindowUrl = App.class.getResource("layout/taskOverviewWindow/task_overview_window.fxml");
    assertThat(taskOverviewWindowUrl).as("Cannot resolve task overview window FXML resource!").isNotNull();

    FXMLLoader fxmlLoader = new FXMLLoader(taskOverviewWindowUrl);
    fxmlLoader.setController(new TaskOverviewWindowController(taskManager));
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.show();

    taskList = (TreeView<GuiTask>) root.lookup("#taskList");
  }

  @BeforeAll
  static void setup() {
    taskManager = new TaskManager();
  }

  @BeforeEach
  void reset() {
    taskManager.clearTasks();
    waitForFxEvents();
  }

  /**
   * We check, whether our implementation of {@link TaskOverviewWindow#show()} actually displays the window.
   */
  @Test
  void isWindowShown() {
    FxHelper.runFxSafe(() -> {
      TaskOverviewWindow testWindow = TaskOverviewWindow.getInstance(taskManager);
      testWindow.show();

      assertThat(testWindow.getStage().isShowing()).isTrue();
    });
  }

  @Test
  void shouldShowTaskWhenTaskAdded() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task");
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(displayedTasks()).contains(task);
  }

  @Test
  void shouldNotShowTaskWhenTaskRemoved() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task");
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(displayedTasks()).contains(task);

    taskManager.removeTask(task);
    waitForFxEvents();

    assertThat(displayedTasks()).isEmpty();
  }

  /**
   * Ensures that the list of tasks stays coherent with the actual tasks.
   */
  @Test
  void listContentsAreCoherent() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task");
    ProgressBarTask task2 = new ProgressBarTask(taskManager, "task-2", "Test Task 2");
    ProgressBarTask task3 = new ProgressBarTask(taskManager, "task-3", "Test Task 3");

    taskManager.addTask(task1);
    waitForFxEvents();
    taskManager.addTask(task2);
    waitForFxEvents();
    taskManager.addTask(task3);
    waitForFxEvents();

    assertThat(displayedTasks()).containsExactly(task1, task2, task3);
  }

  /**
   * Our TaskOverViewWindow should reuse the same window instance if it is already open. Also, the existing window should be brought to the front
   */
  @Test
  void reusesExistingWindow() {

    FxHelper.runFxSafe(() -> {

      TaskOverviewWindow testWindow1 = TaskOverviewWindow.getInstance(taskManager);
      testWindow1.show();

      TaskOverviewWindow testWindow2 = TaskOverviewWindow.getInstance(taskManager);
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
  void showsEmptyListWhenNoTasks() {

    // In @BeforeEach tasks get cleared before each test
    assertThat(displayedTasks()).isEmpty();
  }

  /**
   * tests whether progress tasks are properly updated in the list when the properties update
   */
  @Test
  void testTaskProgressUpdatesProperly() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task", 100, "Units", 1);
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(displayedTasks()).as("Task should be in the list").contains(task);

    //Test stepBy (includes doStepBy; see implementation of stepBy())
    task.stepBy(20);
    waitForFxEvents();

    assertThat(task.currentProgressProperty().getValue()).as("Progress should be 20").isEqualTo(20);
    assertThat(displayedTasks().getFirst().progressProperty().get()).as("Progress fraction should be 0.2").isEqualTo(0.2);
    assertThat(displayedTasks().getFirst().detailProperty().get()).as("Detail should report the progress").isEqualTo("[20/100 Units]");

    //Test doStepTo (only used internally)
    task.doStepTo(40);
    waitForFxEvents();

    assertThat(task.currentProgressProperty().getValue()).as("Progress should be 40").isEqualTo(40);
    assertThat(displayedTasks().getFirst().progressProperty().get()).as("Progress fraction should be 0.4").isEqualTo(0.4);
  }

  /**
   * The window is resizable, so its content has to follow. The tree must fill the window rather than keeping the width it was laid out with.
   */
  @Test
  void treeFillsTheWindowWhenResized() {

    interact(() -> {
      Stage stage = (Stage) taskList.getScene().getWindow();
      stage.setWidth(700);
      stage.setHeight(500);
    });
    waitForFxEvents();

    double sceneWidth = taskList.getScene().getWidth();
    double sceneHeight = taskList.getScene().getHeight();

    assertThat(taskList.getWidth()).as("the tree must grow with the window, not stay at its initial width").isEqualTo(sceneWidth);
    assertThat(taskList.getHeight()).as("the tree must fill the window vertically too").isEqualTo(sceneHeight);
  }

  /**
   * A task row has to use the whole width of the tree, otherwise its progress bar stays stuck at the width it was first laid out with.
   */
  @Test
  void taskRowFillsTheTreeWidth() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Downloading", 100, "MiB", 1);
    taskManager.addTask(task);
    waitForFxEvents();

    interact(() -> {
      Stage stage = (Stage) taskList.getScene().getWindow();
      stage.setWidth(700);
    });
    waitForFxEvents();

    Node row = taskList.lookup(".task-cell");
    assertThat(row).as("the task row should be rendered").isNotNull();
    // The row sits inside the cell, which carries padding and (for sub-steps) an indent, so it cannot match the tree width exactly - but it must be close.
    assertThat(row.getBoundsInParent().getWidth()).as("the row should span the tree rather than only its own content")
        .isGreaterThan(taskList.getWidth() - 60);
  }

  /**
   * A step can be expanded to reveal its sub-steps, so every sub-step must reach the tree as a child of its root - in the order it was started.
   */
  @Test
  void shouldShowSubStepsBelowTheirRoot() {

    IdeGuiContext context = newGuiContext();
    GuiStep root = context.newStep(false, "root");
    waitForFxEvents();

    assertThat(itemOf(root).getChildren()).as("a step without sub-steps has no children to expand").isEmpty();

    GuiStep first = context.newStep(false, "first");
    first.success();
    first.close();
    GuiStep second = context.newStep(false, "second");
    waitForFxEvents();

    assertThat(itemOf(root).getChildren().stream().map(TreeItem::getValue).toList())
        .as("sub-steps appear in the order they started, newest last").containsExactly(first, second);

    second.success();
    second.close();
    root.success();
    root.close();
    waitForFxEvents();

    assertThat(itemOf(root).getChildren()).as("sub-steps stay after they ended, so their outcome remains visible").hasSize(2);
  }

  /**
   * A progress bar reports a single quantity, so it must never offer an expander.
   */
  @Test
  void progressBarHasNoSubTasks() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task", 100, "Units", 1);
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(task.getSubTasks()).isEmpty();
    assertThat(itemOf(task).getChildren()).isEmpty();
  }

  /**
   * The reason for using a {@link TreeView}: expansion belongs to the tree item, which is never recycled, so adding tasks around an expanded one cannot move
   * the expansion to a different row.
   */
  @Test
  void expansionStaysWithItsOwnTask() {

    IdeGuiContext context = newGuiContext();
    GuiStep expanded = context.newStep(false, "expanded");
    context.newStep(false, "sub").close();
    waitForFxEvents();

    itemOf(expanded).setExpanded(true);

    for (int i = 0; i < 20; i++) {
      taskManager.addTask(new ProgressBarTask(taskManager, "filler-" + i, "Filler " + i));
    }
    waitForFxEvents();

    assertThat(itemOf(expanded).isExpanded()).as("the expanded task keeps its state").isTrue();
    assertThat(taskList.getRoot().getChildren().stream().filter(TreeItem::isExpanded).map(TreeItem::getValue).toList())
        .as("no other row became expanded").containsExactly(expanded);
  }

  /**
   * @return a context for creating steps. It is given an empty working directory on purpose: passing {@code null} would make
   *     {@link com.devonfw.tools.ide.context.AbstractIdeContext} fall back to {@code user.dir} and scan the developer's real machine for IDE_HOME.
   */
  private IdeGuiContext newGuiContext() {

    IdeStartContextImpl startContext = new IdeStartContextImpl(IdeLogLevel.DEBUG, new IdeLogListenerBuffer());
    return new IdeGuiContext(startContext, workingDirectory, taskManager);
  }

  /**
   * A progress bar is short-lived: it removes itself from the task list when it is closed, unlike a step which stays until dismissed.
   */
  @Test
  void progressBarRemovesItselfOnClose() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task", 100, "Units", 1);
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(displayedTasks()).contains(task);
    assertThat(task.isDismissable()).as("a progress bar disappears on its own and is never dismissable").isFalse();

    task.close();
    waitForFxEvents();

    assertThat(displayedTasks()).isEmpty();
  }

  /**
   * The CLI reports progress in 1 KiB chunks, so a large archive produces hundreds of thousands of updates. They must be coalesced rather than each posted to
   * the FX thread, but coalescing must never drop the final value - otherwise the bar would stop short of completion.
   */
  @Test
  void rapidProgressUpdatesAreCoalescedWithoutLosingTheFinalValue() throws InterruptedException {

    int steps = 20000;
    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Copying", steps, "Units", 1);
    taskManager.addTask(task);
    waitForFxEvents();

    AtomicInteger uiUpdates = new AtomicInteger();
    task.currentProgressProperty().addListener((_, _, _) -> uiUpdates.incrementAndGet());

    Thread worker = new Thread(() -> {
      for (int i = 0; i < steps; i++) {
        task.stepBy(1);
      }
    });
    worker.start();
    worker.join();
    waitForFxEvents();

    assertThat(task.getCurrentProgress()).as("every step must be counted").isEqualTo(steps);
    assertThat(task.currentProgressProperty().get()).as("the last reported value must still reach the UI").isEqualTo(steps);
    assertThat(task.progressProperty().get()).isEqualTo(1.0);
    assertThat(task.detailProperty().get()).isEqualTo("[" + steps + "/" + steps + " Units]");
    // The freeze this guards against was one FX event posted per reported step, each carrying its own value and so flooding the event queue.
    assertThat(uiUpdates.get()).as("progress updates must be coalesced instead of posting one FX event per step").isLessThan(steps / 10);
  }

  /**
   * An indeterminate progress bar has no quantifiable maximum, so it must not claim a bogus "0 of 100" progress.
   */
  @Test
  void indeterminateTaskHasNoDetail() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Test Task");
    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(task.isIndeterminate()).isTrue();
    assertThat(task.detailProperty().get()).isEmpty();
    assertThat(task.displayTextProperty().get()).isEqualTo("Test Task");
    assertThat(task.progressProperty().get()).isEqualTo(GuiTaskModel.INDETERMINATE);
  }

  /**
   * We check here that a null node reference is handled properly and leads to the window being displayed in the center of the screen.
   */
  @Test
  void testNullReferenceNode() {

    FxHelper.runFxSafe(() -> {
      TaskOverviewWindow nullRefWindow = TaskOverviewWindow.getInstance(taskManager);
      nullRefWindow.showRelativeToReferenceNode(null);

      Rectangle2D screenMeasures = Screen.getPrimary().getVisualBounds();

      double expectedPositionX = screenMeasures.getWidth() / 2 - nullRefWindow.getStage().getScene().getWidth() / 2;
      double expectedPositionY = screenMeasures.getHeight() / 2 - nullRefWindow.getStage().getScene().getHeight() / 2;

      assertThat(nullRefWindow.getStage().getX()).as("Window should be in the expected X position").isEqualTo(expectedPositionX);
      assertThat(nullRefWindow.getStage().getY()).as("Window should be in the expected Y position").isEqualTo(expectedPositionY);
    });
  }
}
