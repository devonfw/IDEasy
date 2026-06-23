package com.devonfw.ide.gui;

import static org.testfx.assertions.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
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
import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;

/**
 * Basic UI Test
 */
public class AppBaseTest extends HeadlessApplicationTest {

  private static final Logger LOG = LoggerFactory.getLogger(AppBaseTest.class);

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

    URL mainViewUrl = getClass().getResource("main-view.fxml");
    assertThat(mainViewUrl).as("Cannot resolve main UI FXML resource!").isNotNull();

    FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
    fxmlLoader.setController(new MainController(mockIdeRoot.toString(), guiStateManager));
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
  protected static void generateProjectFolderStructure() throws IOException {

    LOG.debug("tempDir: {}", mockIdeRoot);
    FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(mockIdeRoot);
    LOG.debug("project folders: {}", Arrays.toString(mockIdeRoot.toFile().list()));

    guiStateManager = new GuiStateManager(taskManager, mockIdeRoot.toString());
    //We set the project root directory to the temporary directory before all tests so that the IDE can find the projects in the test.
    guiStateManager.switchContext("project-1", "main");
  }

  @BeforeEach
  protected void resetTaskManager() {

    taskManager.getTasks().clear();
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
  public void testIdeOpenButtonsEnabledWhenProjectSelected() {

    // assert that project and workspace are selected
    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));

    // assert all IDE open buttons are enabled
    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled()).as(button.getId() + " button should be enabled when a workspace has been selected").isFalse();
    }
  }

  /**
   * Tests that the workspace {@link ComboBox} is disbaled when no project is selected.
   */
  @Test
  public void testWorkspaceComboBoxDisabledWhenNoProjectSelected() {

    assertThat(selectedProject.getValue()).isNull();

    assertThat(selectedWorkspace.isDisabled())
        .as("selectedWorkspace ComboBox should be disabled when no project is selected")
        .isTrue();
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

  @Test
  protected void testStatusLabelDisplaysCorrectMessage() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task");
    ProgressBarTask task2 = new ProgressBarTask(taskManager, "task-2", "Test Task");

    //Case 1: No tasks added yet, check correct message
    assertThat(statusText.getText()).isEqualTo("IDEasy is ready.");

    //Case 2: Only single task exists, should display the task title and a progress bar next to the label
    taskManager.addTask(task1);
    waitForFxEvents();

    assertThat(statusText.getText()).isEqualTo(
        String.format(ProgressBarTask.TASK_DESCRIPTION_STRING_FORMAT,
            task1.getTitle(),
            task1.getCurrentProgress(),
            task1.getMaxSize(),
            task1.getUnitName())
    );
    assertThat(taskProgressBar.isVisible()).as("Task progress bar should be visible").isTrue();

    //Case 3: Multiple tasks exist, should display the number of tasks and a progress bar next to the label
    taskManager.addTask(task2);
    waitForFxEvents();

    assertThat(statusText.getText()).isEqualTo(String.format("%d tasks running...", taskManager.getTasks().size()));
    assertThat(taskProgressBar.isVisible()).as("Task progress bar should not be visible").isFalse();

    //...and back to the default state:
    taskManager.getTasks().clear();
    waitForFxEvents();

    assertThat(statusText.getText()).isEqualTo("IDEasy is ready.");
  }

  @Test
  protected void testStatusTextOpensTaskOverviewWindow() {

    ProgressBarTask task1 = new ProgressBarTask(taskManager, "task-1", "Test Task");
    ProgressBarTask task2 = new ProgressBarTask(taskManager, "task-2", "Test Task");

    taskManager.addTask(task1);
    taskManager.addTask(task2);
    waitForFxEvents();

    interact(() -> statusText.fireEvent(
        new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, null, 1, false, false, false, false, false, false, false, false, false, false, null)));

    assertThat(TaskOverviewWindow.getInstance(taskManager).getStage().isShowing()).as("Task overview window should be opened when clicking on status text")
        .isTrue();
  }
}
