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

import com.devonfw.ide.gui.console.ConsoleController;
import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Basic UI Test for the main screen
 */
public class AppBaseTest extends HeadlessApplicationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppBaseTest.class);

  private Button androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen;
  private ToggleButton consolePaneToggleButton;
  private ComboBox<String> selectedProject, selectedWorkspace;
  private Label statusText;
  private ProgressBar taskProgressBar;
  private SplitPane centerSplitPane;
  private ConsoleController originalConsole;
  private Divider originalCenterDivider;

  private MainController mainController;

  @TempDir
  private static Path mockIdeRoot;

  private static final TaskManager taskManager = new TaskManager();
  private static GuiStateManager guiStateManager;

  @Override
  public void start(Stage stage) throws IOException {

    NlsService nlsService = new NlsService(Locale.ENGLISH);

    URL mainViewUrl = getClass().getResource("main-view.fxml");
    assertThat(mainViewUrl).as("Cannot resolve main UI FXML resource!").isNotNull();

    this.mainController = new MainController(mockIdeRoot.toString(), guiStateManager, nlsService, null);
    final ConsoleController[] createdConsole = new ConsoleController[1];
    FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
    fxmlLoader.setControllerFactory(clazz -> {
      if (clazz == ConsoleController.class) {
        createdConsole[0] = new ConsoleController(nlsService);
        return createdConsole[0];
      } else if (clazz == MainController.class) {
        return this.mainController;
      }
      return null;
    });
    fxmlLoader.setResources(nlsService.getResourceBundle());
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus(); //sometimes needed for headless setup to work
    stage.show();

    androidStudioOpen = FxHelper.lookup(root, "#androidStudioOpen");
    eclipseOpen = FxHelper.lookup(root, "#eclipseOpen");
    intellijOpen = FxHelper.lookup(root, "#intellijOpen");
    vsCodeOpen = FxHelper.lookup(root, "#vsCodeOpen");
    selectedProject = FxHelper.lookup(root, "#selectedProject");
    selectedWorkspace = FxHelper.lookup(root, "#selectedWorkspace");
    consolePaneToggleButton = FxHelper.lookup(root, "#consolePaneToggleButton");
    centerSplitPane = FxHelper.lookup(root, "#centerSplitPane");
    originalCenterDivider = centerSplitPane.getDividers().getFirst();
    originalConsole = createdConsole[0];
    statusText = FxHelper.lookup(root, "#statusLabel");
    taskProgressBar = FxHelper.lookup(root, "#statusProgressBar");
  }

  /**
   * Generate temporary project directories to be able to test on any device (including GitHub CI). This is required for the {@link MainController} to work in
   * the test context. Generates a structure like this: /project-[0..5]/workspaces/main
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

  //===Console panel tests===

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

  /**
   * This test ensures that switching languages won't reset the GUI-State
   */
  @Test
  public void testLanguageSelectionDoesNotResetGuiState() throws IOException {

    // Arrange
    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));

    final long[] originalLineTimestamp = new long[1];
    interact(() -> {
      originalConsole.appendOutput(IdeLogLevel.INFO, "restored line");
      originalConsole.setAutoScrollEnabled(false);
      originalCenterDivider.setPosition(0.75);
      originalLineTimestamp[0] = originalConsole.getLogEntries().stream()
          .filter(entry -> entry.message().contains("restored line"))
          .findFirst().orElseThrow().timeStamp();
    });
    waitForFxEvents();

    // Act
    ReloadedView reloaded = reloadMainViewOnFxThread(Locale.GERMAN, guiStateManager, mainController);
    waitForFxEvents();

    // Assert
    assertThat(reloaded.project.getValue()).as("Project should be restored after switching language").isEqualTo("project-1");
    assertThat(reloaded.workspace.getValue()).as("Workspace should be restored after switching language").isEqualTo("main");
    for (Button button : reloaded.ideButtons) {
      assertThat(button.isDisabled()).as(button.getId() + " button should remain enabled after switching language").isFalse();
    }

    assertThat(reloaded.selectedLanguage.getValue())
        .as("The reloaded language combo should reflect the newly applied locale")
        .isEqualTo(new NlsService(Locale.GERMAN).getLanguageDisplayName(Locale.GERMAN));

    assertThat(reloaded.console.getConsoleOutputSnapshot()).as("Console output should be restored after switching language")
        .anyMatch(line -> line.contains("restored line"));
    assertThat(reloaded.console.getLogEntries()).as("Console log entries should be restored after switching language")
        .anySatisfy(entry -> {
          assertThat(entry.message()).as("Restored console line").contains("restored line");
          assertThat(entry.level()).as("Restored console line should keep its original level").isEqualTo(IdeLogLevel.INFO);
          assertThat(entry.timeStamp()).as("Restored console line should keep its original timestamp")
              .isEqualTo(originalLineTimestamp[0]);
        });
    assertThat(reloaded.console.isAutoScrollEnabled()).as("Console auto-scroll setting should be restored after switching language").isFalse();
    assertThat(reloaded.centerDivider.getPosition()).as("Console pane visibility should be restored after switching language")
        .isEqualTo(0.75, Offset.offset(0.01));
    assertThat(reloaded.consolePaneToggleButton.isSelected()).as("Console toggle button should reflect the restored pane visibility").isTrue();

  }


  /**
   * Verifies that {@link MainController#dispose()} detaches the controller from its task list.
   */
  @Test
  public void testDisposedControllerNoLongerReactsToTaskChanges() {

    // Arrange
    TaskManager isolatedTaskManager = new TaskManager();
    GuiStateManager isolatedStateManager = new GuiStateManager(isolatedTaskManager, mockIdeRoot.toString());
    ReloadedView reloaded = reloadMainViewOnFxThread(Locale.ENGLISH, isolatedStateManager, null);
    Label statusLabel = reloaded.statusLabel;

    // Act
    isolatedTaskManager.addTask(new ProgressBarTask(isolatedTaskManager, "task-1", "Task 1"));
    waitForFxEvents();
    String textWhileActive = statusLabel.getText();

    // Act
    reloaded.controller.dispose();
    isolatedTaskManager.clearTasks();
    waitForFxEvents();

    // Assert
    assertThat(textWhileActive).as("The controller should have reacted to a task change while active").isNotEqualTo("IDEasy is ready.");
    assertThat(statusLabel.getText())
        .as("A disposed controller must no longer react to task-list changes")
        .isEqualTo(textWhileActive);
  }

  /**
   * Verifies that after the GUI re-loads the main view (as it does on a language change) the re-loaded view is fully functional AND no longer retains the
   * previous controller.
   */
  @Test
  public void testReloadReleasesPreviousController() throws IOException {

    // Arrange
    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));
    waitForFxEvents();

    MainController original = mainController;

    // Act
    ReloadedView reloaded = reloadMainViewOnFxThread(Locale.GERMAN, guiStateManager, original);
    waitForFxEvents();

    try {
      // Assert
      assertThat(reloaded.project.getValue()).as("Project should be restored after reload").isEqualTo("project-1");
      assertThat(reloaded.workspace.getValue()).as("Workspace should be restored after reload").isEqualTo("main");
      assertThat(readField(reloaded.controller, "oldMainController"))
          .as("The re-loaded controller must not retain the previous controller after restoring its state")
          .isNull();
    } finally {
      reloaded.controller.dispose();
    }
  }

  /**
   * Reloads the main view on the FX thread the same way the GUI does on a locale change: a new {@link MainController} is constructed with the given previous
   * controller handed over as its {@code oldMainController}, and the FXML view is re-loaded with the given locale. This forces the new controller's
   * {@code initialize} to re-apply the previous controller's selection.
   *
   * @param locale the locale to apply on the re-loaded view
   * @param guiStateManager the {@link GuiStateManager} the new controller is wired to
   * @param oldController the controller holding the selection to carry over, or {@code null} for a first load
   * @return the relevant nodes of the freshly loaded main view
   */
  private ReloadedView reloadMainViewOnFxThread(Locale locale, GuiStateManager guiStateManager, MainController oldController) {

    final ReloadedView[] reloaded = new ReloadedView[1];
    interact(() -> {
      try {
        NlsService nlsService = new NlsService(locale);
        MainController controller = new MainController(mockIdeRoot.toString(), guiStateManager, nlsService, oldController);
        final ConsoleController[] createdConsole = new ConsoleController[1];

        URL mainViewUrl = AppBaseTest.class.getResource("main-view.fxml");
        assertThat(mainViewUrl).as("Cannot resolve main UI FXML resource!").isNotNull();
        FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
        fxmlLoader.setControllerFactory(clazz -> {
          if (clazz == ConsoleController.class) {
            createdConsole[0] = new ConsoleController(nlsService);
            return createdConsole[0];
          } else if (clazz == MainController.class) {
            return controller;
          }
          return null;
        });
        fxmlLoader.setResources(nlsService.getResourceBundle());
        Parent root = fxmlLoader.load();

        reloaded[0] = new ReloadedView();
        reloaded[0].controller = controller;
        reloaded[0].project = (ComboBox<String>) root.lookup("#selectedProject");
        reloaded[0].workspace = (ComboBox<String>) root.lookup("#selectedWorkspace");
        reloaded[0].selectedLanguage = (ComboBox<String>) root.lookup("#selectedLanguage");
        reloaded[0].ideButtons = new Button[] { readButtonField(controller, "androidStudioOpen"), readButtonField(controller, "eclipseOpen"),
            readButtonField(controller, "intellijOpen"), readButtonField(controller, "vsCodeOpen") };
        reloaded[0].console = createdConsole[0];
        reloaded[0].consolePaneToggleButton = (ToggleButton) root.lookup("#consolePaneToggleButton");
        reloaded[0].centerDivider = ((SplitPane) root.lookup("#centerSplitPane")).getDividers().getFirst();
        reloaded[0].statusLabel = (Label) root.lookup("#statusLabel");
      } catch (IOException e) {
        throw new IllegalStateException("Failed to load the main view", e);
      }
    });
    return reloaded[0];
  }

  /**
   * Reads a {@link MainController} {@code @FXML}-injected button field. The IDE open buttons are nested inside a {@link javafx.scene.control.ScrollPane}, so
   * {@link javafx.scene.Node#lookup(String)} cannot reach them on a freshly loaded view that has not yet been laid out (its content is only added to the node
   * tree after the view is shown/leveled). The FXML-injected controller field is already populated after loading, so it is read instead.
   *
   * @param controller the {@link MainController} of the loaded view
   * @param fieldName the name of the FXML field to read
   * @return the button assigned to the given field
   */
  private static Button readButtonField(MainController controller, String fieldName) {

    try {
      java.lang.reflect.Field field = MainController.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return (Button) field.get(controller);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to read " + fieldName, e);
    }
  }

  /**
   * Reads a (possibly private) declared field by name via reflection. Used to inspect {@link MainController} state that is not otherwise observable.
   *
   * @param target the object whose field is read
   * @param fieldName the name of the declared field to read
   * @return the current value of the field
   */
  private static Object readField(Object target, String fieldName) {

    try {
      java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to read " + fieldName, e);
    }
  }

  /**
   * The project, workspace, IDE open button and console nodes of a loaded main view.
   */
  private static final class ReloadedView {

    private MainController controller;
    private ComboBox<String> project;
    private ComboBox<String> workspace;
    private ComboBox<String> selectedLanguage;
    private Button[] ideButtons;
    private ConsoleController console;
    private ToggleButton consolePaneToggleButton;
    private Divider centerDivider;
    private Label statusLabel;
  }
}
