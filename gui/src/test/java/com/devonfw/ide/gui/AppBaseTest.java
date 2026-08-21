package com.devonfw.ide.gui;

import static org.testfx.assertions.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
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

/**
 * Basic UI Test
 */
public class AppBaseTest extends HeadlessApplicationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppBaseTest.class);

  private Button androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen;
  private ComboBox<String> selectedProject, selectedWorkspace;
  private Label statusText;
  private ProgressBar taskProgressBar;

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

    mainController = new MainController(mockIdeRoot.toString(), guiStateManager, nlsService, null);
    FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
    fxmlLoader.setController(mainController);
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

  /**
   * Regression test for #2040: switching between projects must reset the workspace selection and keep the IDE open buttons and the context consistent.
   * <p>
   * Previously the workspace selection and IDE open buttons were not kept in sync when a different project was selected. This ensures that selecting a new
   * project clears the workspace selection and disables the IDE open buttons again, and that (re-)selecting the workspace of the new project re-enables the
   * buttons and points the context to the correct project.
   */
  @Test
  public void testSwitchingProjectResetsWorkspaceSelection() {

    // select a project and its workspace -> all IDE open buttons become enabled
    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));

    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled()).as(button.getId() + " button should be enabled when a project and workspace are selected").isFalse();
    }

    // switch to another project -> the workspace selection must be reset and the IDE open buttons disabled again
    interact(() -> selectedProject.getSelectionModel().select("project-2"));

    assertThat(selectedWorkspace.getValue()).as("Workspace selection should be reset when switching to a different project").isNull();

    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled())
          .as(button.getId() + " button should be disabled after switching to a new project without a selected workspace").isTrue();
    }

    // re-select the workspace of the new project -> buttons enabled again and the context points to the correct project
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));

    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled())
          .as(button.getId() + " button should be enabled again after selecting the workspace of the new project").isFalse();
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
   * Regression test for #2214: switching the application language re-loads the main view and constructs a fresh {@link MainController}.
   * <p>
   * The user's selection (project, workspace) and the enabled state of the IDE open buttons must survive the reload. This drives the reload the same way the
   * GUI does on a locale change: a new {@link MainController} is loaded, with the existing controller's selection handed over as its {@code oldMainController}.
   * It also verifies that a project which no longer exists after the reload is skipped gracefully instead of being reapplied.
   */
  @Test
  public void testLanguageSelectionDoesNotResetGuiState() throws IOException {

    // Arrange: select a project and its workspace -> the IDE open buttons become enabled.
    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));

    // Act: reload the main view with a new locale, carrying over the existing selection (as the GUI does on a locale change).
    ReloadedView reloaded = reloadMainViewOnFxThread(Locale.GERMAN, mainController);

    // Assert: project, workspace and the enabled state of the IDE open buttons are preserved.
    assertThat(reloaded.project.getValue()).as("Project should be restored after switching language").isEqualTo("project-1");
    assertThat(reloaded.workspace.getValue()).as("Workspace should be restored after switching language").isEqualTo("main");
    for (Button button : reloaded.ideButtons) {
      assertThat(button.isDisabled()).as(button.getId() + " button should remain enabled after switching language").isFalse();
    }

    // Act: select another project (unused by the other tests in this class) and then remove it so it no longer exists -> reload again.
    interact(() -> selectedProject.getSelectionModel().select("project-4"));
    deleteProject("project-4");
    ReloadedView stale = reloadMainViewOnFxThread(Locale.ENGLISH, mainController);

    // Assert: the removed project is not reapplied and the buttons stay disabled.
    assertThat(stale.project.getItems()).as("The removed project should not be present in the reloaded project list").doesNotContain("project-4");
    assertThat(stale.project.getValue()).as("A no-longer-existing project should not be restored").isNull();
    assertThat(stale.workspace.getValue()).as("Workspace should not be restored for a removed project").isNull();
    for (Button button : stale.ideButtons) {
      assertThat(button.isDisabled()).as(button.getId() + " button should be disabled when no project is selected").isTrue();
    }
  }

  /**
   * Reloads the main view on the FX thread the same way the GUI does on a locale change: a new {@link MainController} is constructed with the given previous
   * controller handed over as its {@code oldMainController}, and the FXML view is re-loaded with the given locale. This forces the new controller's
   * {@code initialize} to re-apply the previous controller's selection.
   *
   * @param locale the locale to apply on the re-loaded view
   * @param oldController the controller holding the selection to carry over
   * @return the relevant nodes of the freshly loaded main view
   */
  private ReloadedView reloadMainViewOnFxThread(Locale locale, MainController oldController) {

    final ReloadedView[] reloaded = new ReloadedView[1];
    interact(() -> {
      try {
        NlsService nlsService = new NlsService(locale);
        MainController controller = new MainController(mockIdeRoot.toString(), guiStateManager, nlsService, oldController);

        URL mainViewUrl = AppBaseTest.class.getResource("main-view.fxml");
        assertThat(mainViewUrl).as("Cannot resolve main UI FXML resource!").isNotNull();
        FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
        fxmlLoader.setController(controller);
        fxmlLoader.setResources(nlsService.getResourceBundle());
        Parent root = fxmlLoader.load();

        reloaded[0] = new ReloadedView();
        reloaded[0].project = (ComboBox<String>) root.lookup("#selectedProject");
        reloaded[0].workspace = (ComboBox<String>) root.lookup("#selectedWorkspace");
        reloaded[0].ideButtons = new Button[] { (Button) root.lookup("#androidStudioOpen"), (Button) root.lookup("#eclipseOpen"),
            (Button) root.lookup("#intellijOpen"), (Button) root.lookup("#vsCodeOpen") };
      } catch (IOException e) {
        throw new IllegalStateException("Failed to load the main view", e);
      }
    });
    return reloaded[0];
  }

  /**
   * The project, workspace and IDE open button nodes of a loaded main view.
   */
  private static final class ReloadedView {

    private ComboBox<String> project;
    private ComboBox<String> workspace;
    private Button[] ideButtons;
  }

  /**
   * Removes the given project directory so it no longer shows up in the project list.
   *
   * @param projectName the name of the project to remove
   */
  private void deleteProject(String projectName) throws IOException {

    Files.deleteIfExists(mockIdeRoot.resolve(projectName).resolve("workspaces").resolve("main"));
    Files.deleteIfExists(mockIdeRoot.resolve(projectName).resolve("workspaces"));
    Files.deleteIfExists(mockIdeRoot.resolve(projectName));
  }
}
