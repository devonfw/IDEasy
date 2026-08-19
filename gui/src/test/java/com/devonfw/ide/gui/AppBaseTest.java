package com.devonfw.ide.gui;

import static org.testfx.assertions.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;
import com.devonfw.tools.ide.step.Step;

/**
 * Basic UI Test
 */
public class AppBaseTest extends HeadlessApplicationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppBaseTest.class);

  private Button androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen;
  private ComboBox<String> selectedProject, selectedWorkspace;
  private Label statusText;
  private ProgressBar taskProgressBar;

  @TempDir
  private static Path mockIdeRoot;

  private static final TaskManager taskManager = new TaskManager();
  private static GuiStateManager guiStateManager;

  @Override
  public void start(Stage stage) throws IOException {

    NlsService nlsService = new NlsService(Locale.ENGLISH);

    URL mainViewUrl = getClass().getResource("layout/mainview/main-view.fxml");
    assertThat(mainViewUrl).as("Cannot resolve main UI FXML resource!").isNotNull();

    FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
    fxmlLoader.setController(new MainController(mockIdeRoot.toString(), guiStateManager, nlsService));
    fxmlLoader.setResources(nlsService.getResourceBundle());
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus(); //sometimes needed for headless setup to work
    stage.show();

    androidStudioOpen = (Button) root.lookup("#androidStudioOpen");
    eclipseOpen = (Button) root.lookup("#eclipseOpen");
    intellijOpen = (Button) root.lookup("#intellijOpen");
    vsCodeOpen = (Button) root.lookup("#vsCodeOpen");
    selectedProject = (ComboBox<String>) root.lookup("#selectedProject");
    selectedWorkspace = (ComboBox<String>) root.lookup("#selectedWorkspace");
    statusText = (Label) root.lookup("#statusLabel");
    taskProgressBar = (ProgressBar) root.lookup("#statusProgressBar");
  }

  /**
   * Generate temporary project directories to be able to test on any device (including GitHub CI). This is required for the {@link MainController} to work in
   * the test context. Generates a structure like this: /project-[0..6]/workspaces/main
   */
  @BeforeAll
  public static void generateProjectFolderStructure() throws IOException {

    LOGGER.debug("tempDir: {}", mockIdeRoot);
    FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(mockIdeRoot);
    LOGGER.debug("project folders: {}", Arrays.toString(mockIdeRoot.toFile().list()));

    guiStateManager = new GuiStateManager(taskManager, mockIdeRoot.toString());
    //We set the project root directory to the temporary directory before all tests so that the IDE can find the projects in the test.
    guiStateManager.switchContext("project-1", "main");
  }

  @BeforeEach
  protected void resetTaskManager() {

    taskManager.clearTasks();
    waitForFxEvents();
  }

  /**
   * Tests that the workspace {@link ComboBox} is enabled when a project is selected.
   */
  @Test
  public void testWorkspaceComboboxEnabledEnabledWhenProjectSelected() {

    // assert that a project is selected
    interact(() -> selectedProject.getSelectionModel().select("project-1"));

    // assert all IDE open buttons are disabled
    assertThat(selectedWorkspace.isDisabled())
        .as("selectedWorkspace ComboBox should be enabled when a project is selected")
        .isFalse();
  }

  /**
   * This test ensures that all IDE open buttons are disabled when no project is selected.
   */
  @Test
  public void testIdeOpenButtonsDisabledWhenNoProjectSelected() {

    // assert that no project is selected
    assertThat(selectedProject.getValue()).isNull();

    // assert all IDE open buttons are disabled
    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled()).as(button.getId() + " button should be disabled when no project has been selected").isTrue();
    }
  }

  /**
   * This test ensures that all IDE open buttons are enabled when a project is selected.
   */
  @Test
  public void testIdeOpenButtonsEnabledWhenWorkspaceSelected() {

    // assert that a project and workspace is selected
    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));

    // assert all IDE open buttons are enabled
    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled()).as(button.getId() + " button should be enabled when a workspace has been selected").isFalse();
    }
  }

  @Test
  protected void testStatusLabelDisplaysCorrectMessage() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task");
    ProgressBarTask task2 = new ProgressBarTask(taskManager, "task-2", "Test Task");

    //Case 1: No tasks added yet, check correct message
    assertThat(statusText.getText()).isEqualTo("IDEasy is ready.");

    //Case 2: Only single task exists, should display the task title and a progress bar next to the label
    taskManager.addTask(task1);
    waitForFxEvents();

    // task1 is indeterminate, so there is no "x of y" detail to append to the title.
    assertThat(statusText.getText()).isEqualTo(task1.displayTextProperty().get());
    assertThat(taskProgressBar.isVisible()).as("Task progress bar should be visible").isTrue();

    //Case 3: Multiple tasks exist, should display the number of tasks and a progress bar next to the label
    taskManager.addTask(task2);
    waitForFxEvents();

    assertThat(statusText.getText()).isEqualTo(String.format("%d tasks running...", taskManager.getTasks().size()));
    assertThat(taskProgressBar.isVisible()).as("Task progress bar should not be visible").isFalse();

    //...and back to the default state:
    taskManager.clearTasks();
    waitForFxEvents();

    assertThat(statusText.getText()).isEqualTo("IDEasy is ready.");
  }

  /**
   * The status label follows a running task through the extractor of the task list, so progressing a task must update the label without any per-task listener
   * being registered by the controller.
   */
  @Test
  protected void testStatusLabelFollowsTaskProgress() {

    ProgressBarTask task = new ProgressBarTask(taskManager, "task-1", "Downloading", 100, "MiB", 1);

    taskManager.addTask(task);
    waitForFxEvents();

    assertThat(statusText.getText()).isEqualTo("Downloading [0/100 MiB]");

    task.stepBy(25);
    waitForFxEvents();

    assertThat(statusText.getText()).as("status label should follow the progress of the running task").isEqualTo("Downloading [25/100 MiB]");
    assertThat(taskProgressBar.getProgress()).isEqualTo(0.25);
  }

  /**
   * A step stays in the task list when it ends, so its completion is not a structural change of the list. Only the binding on its state can pick that up, which
   * is why the status bar binds to the properties of the tasks rather than reading them from a list listener.
   */
  @Test
  protected void testStatusLabelUpdatesWhenStepFinishesInPlace() {

    Step step = guiStateManager.getCurrentContext().newStep("Doing something");
    waitForFxEvents();

    assertThat(statusText.getText()).isEqualTo("Doing something");
    assertThat(taskProgressBar.isVisible()).as("a running step shows the indeterminate bar").isTrue();

    step.success();
    step.close();
    waitForFxEvents();

    assertThat(statusText.getText()).as("the finished step stays in the list and the status bar must reflect that").isEqualTo("1 tasks finished");
    assertThat(taskProgressBar.isVisible()).as("nothing is running anymore").isFalse();
  }

  @Test
  protected void testStatusTextOpensTaskOverviewWindow() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task");
    ProgressBarTask task2 = new ProgressBarTask(taskManager, "task-2", "Test Task");

    taskManager.addTask(task1);
    waitForFxEvents();
    taskManager.addTask(task2);
    waitForFxEvents();

    interact(() -> statusText.fireEvent(
        new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, null, 1, false, false, false, false, false, false, false, false, false, false, null)));

    assertThat(TaskOverviewWindow.getInstance(taskManager).getStage().isShowing()).as("Task overview window should be opened when clicking on status text")
        .isTrue();
  }
}
