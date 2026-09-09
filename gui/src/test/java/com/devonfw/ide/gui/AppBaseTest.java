package com.devonfw.ide.gui;

import static org.testfx.assertions.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.factory.TabFactory;
import com.devonfw.ide.gui.helper.FxHelper;
import com.devonfw.ide.gui.service.CommandletService;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;
import com.devonfw.ide.gui.ui.mainwindow.MainWindowView;
import com.devonfw.ide.gui.ui.mainwindow.MainWindowViewModel;
import com.devonfw.ide.gui.ui.progress.ProgressBarTask;
import com.devonfw.ide.gui.ui.progress.taskwindow.TaskOverviewWindow;

/**
 * Basic UI Test for the main screen
 */
public class AppBaseTest extends HeadlessApplicationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppBaseTest.class);

  /** Scene size for the test window. The console SplitPane needs real space, or its divider stays pinned near 1.0. */
  private static final double SCENE_WIDTH = 1280;

  private static final double SCENE_HEIGHT = 800;

  private Button androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen;
  private ToggleButton consolePaneToggleButton;
  private ComboBox<String> selectedProject, selectedWorkspace;
  private Label statusText;
  private ProgressBar taskProgressBar;
  private SplitPane centerSplitPane;

  @TempDir
  private static Path mockIdeRoot;

  /**
   * Both are recreated for every test in {@link #start(Stage)}: a shared GuiStateManager leaks the project selection into the next test, and a shared
   * TaskManager accumulates a status-bar listener per view model, so discarded windows keep reacting to task changes.
   */
  private TaskManager taskManager;

  private GuiStateManager guiStateManager;

  private MainWindowViewModel viewModel;

  @Override
  public void start(Stage stage) {

    NlsService nlsService = new NlsService(Locale.ENGLISH);
    this.taskManager = new TaskManager();
    this.guiStateManager = new GuiStateManager(this.taskManager, mockIdeRoot.toString());
    ConsoleController consoleController = new ConsoleController(nlsService);
    CommandletService commandletService = new CommandletService(guiStateManager, consoleController);
    TabFactory tabFactory = new TabFactory(guiStateManager, nlsService, commandletService, consoleController);

    this.viewModel = new MainWindowViewModel(guiStateManager, guiStateManager.getProjectManager(), nlsService);
    MainWindowView mainWindow = new MainWindowView(this.viewModel, guiStateManager, nlsService, consoleController, tabFactory);
    stage.setScene(new Scene(mainWindow, SCENE_WIDTH, SCENE_HEIGHT));
    stage.requestFocus(); //sometimes needed for headless setup to work
    stage.show();

    androidStudioOpen = FxHelper.lookup(mainWindow, "#androidStudioOpen");
    eclipseOpen = FxHelper.lookup(mainWindow, "#eclipseOpen");
    intellijOpen = FxHelper.lookup(mainWindow, "#intellijOpen");
    vsCodeOpen = FxHelper.lookup(mainWindow, "#vsCodeOpen");
    selectedProject = FxHelper.lookup(mainWindow, "#projects");
    selectedWorkspace = FxHelper.lookup(mainWindow, "#workspaces");
    consolePaneToggleButton = FxHelper.lookup(mainWindow, "#consolePaneToggleButton");
    centerSplitPane = FxHelper.lookup(mainWindow, "#centerSplitPane");
    statusText = FxHelper.lookup(mainWindow, "#statusLabel");
    taskProgressBar = FxHelper.lookup(mainWindow, "#statusProgressBar");
  }

  /**
   * Generate temporary project directories to be able to test on any device (including GitHub CI). This is required for the {@link MainWindowView} to work in
   * the test context. Generates a structure like this: /project-[0..6]/workspaces/main
   */
  @BeforeAll
  public static void generateProjectFolderStructure() throws IOException {

    LOGGER.debug("tempDir: {}", mockIdeRoot);
    FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(mockIdeRoot);
    LOGGER.debug("project folders: {}", Arrays.toString(mockIdeRoot.toFile().list()));

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

  /**
   * This test ensures that switching to a project will auto-select the main workspace
   */
  @Test
  public void testSwitchingProjectResetsWorkspaceSelectionToMain() {

    // select a project and its workspace -> all IDE open buttons become enabled
    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));

    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled()).as(button.getId() + " button should be enabled when a project and workspace are selected").isFalse();
    }

    // switch to another project -> the workspace selection must be reset and the IDE open buttons disabled again
    interact(() -> selectedProject.getSelectionModel().select("project-2"));

    assertThat(selectedWorkspace.getValue()).as("Workspace selection should be reset when switching to a different project").isEqualTo("main");

    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled())
          .as(button.getId() + " button should be disabled after switching to a new project without a selected workspace").isFalse();
    }

    assertThat(guiStateManager.getCurrentContext().getCwd().endsWith(Path.of("project-2", "workspaces", "main")))
        .as("Context should point to the main workspace of the newly selected project").isTrue();
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
    taskManager.clearTasks();
    waitForFxEvents();

    assertThat(statusText.getText()).isEqualTo("IDEasy is ready.");
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

  /**
   * The status label now carries a permanently installed click handler that is gated on the view model's clickable state, where it previously had the handler
   * attached only while more than one task was running. This pins that gate.
   */
  @Test
  protected void testStatusTextDoesNotOpenTaskOverviewWindowForSingleTask() {

    // The window is a JVM-wide singleton, so an earlier test may have left it showing. getInstance builds the Stage, so it has to run on the FX thread.
    interact(() -> TaskOverviewWindow.getInstance(this.taskManager).getStage().hide());

    this.taskManager.addTask(new ProgressBarTask(this.taskManager, "task-1", "Test Task"));
    waitForFxEvents();

    interact(() -> statusText.fireEvent(
        new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, null, 1, false, false, false, false, false, false, false, false, false, false, null)));

    assertThat(TaskOverviewWindow.getInstance(this.taskManager).getStage().isShowing())
        .as("Task overview window should not open while only a single task is running").isFalse();
  }

  //===Console panel tests===

  /**
   * The pre-launch action registered on the TabFactory opens the console by calling {@code setConsoleVisible(true)} on the view model. Launching a commandlet
   * for real would install an IDE, so this covers the mechanism that action drives rather than the launch itself.
   */
  @Test
  void testConsoleOpensWhenRequestedThroughTheViewModel() {

    Divider mainPanelDivider = centerSplitPane.getDividers().getFirst();
    assertThat(mainPanelDivider.getPosition()).as("The console should start collapsed").isGreaterThan(0.99);

    interact(() -> this.viewModel.setConsoleVisible(true));
    waitForFxEvents();

    assertThat(mainPanelDivider.getPosition()).as("Console panel should be extended when the view model requests it").isEqualTo(0.75, Offset.offset(0.01));
    assertThat(consolePaneToggleButton.isSelected()).as("The toggle button should follow the view model").isTrue();
  }

  /**
   * Dragging the divider must not be fought by the view writing the position back. Crossing the visible threshold used to snap the divider straight to 0.75.
   */
  @Test
  void testDraggingDividerDoesNotSnapTheConsoleOpen() {

    Divider mainPanelDivider = centerSplitPane.getDividers().getFirst();

    // Just past the point where the console counts as visible, as if the user were dragging it open.
    interact(() -> mainPanelDivider.setPosition(0.985));
    waitForFxEvents();

    assertThat(mainPanelDivider.getPosition()).as("The divider must stay where the drag put it").isEqualTo(0.985, Offset.offset(0.001));
    assertThat(consolePaneToggleButton.isSelected()).as("The console counts as visible past the threshold").isTrue();
  }

  @Test
  void testConsoleToggleButton() {

    Divider mainPanelDivider = centerSplitPane.getDividers().getFirst();

    //open the console (for some reason, clickOn(toggleButton) does not work properly here.
    consolePaneToggleButton.fire();
    waitForFxEvents();

    assertThat(consolePaneToggleButton.isSelected()).isTrue();
    assertThat(mainPanelDivider.getPosition()).as("Console panel should be extended when opening the console").isEqualTo(0.75, Offset.offset(0.01));

    //close the console
    consolePaneToggleButton.fire();
    waitForFxEvents();

    assertThat(consolePaneToggleButton.isSelected()).isFalse();
    assertThat(mainPanelDivider.getPosition()).isGreaterThan(0.99);
  }
}
