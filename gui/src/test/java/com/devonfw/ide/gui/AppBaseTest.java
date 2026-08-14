package com.devonfw.ide.gui;

import static org.testfx.assertions.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
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
import com.devonfw.ide.gui.settings.ToolConfiguration;

/**
 * Basic UI Test
 * <p>
 * Note: TestFX calls {@link #start(Stage)} fresh before every {@code @Test} method (JUnit5's default per-method test instance lifecycle), so no state (combo
 * box selections, loaded tab content, ...) carries over between tests. Every test sets up whatever it needs from scratch, the same way the pre-existing tests
 * below already do.
 */
public class AppBaseTest extends HeadlessApplicationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppBaseTest.class);

  private Button androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen;
  private ComboBox<String> selectedProject, selectedWorkspace;
  private Label statusText;
  private ProgressBar taskProgressBar;
  private TabPane tabPane;
  private Tab mainTab, toolConfigTab;

  @TempDir
  private static Path mockIdeRoot;

  private static final TaskManager taskManager = new TaskManager();
  private static GuiStateManager guiStateManager;

  @Override
  public void start(Stage stage) throws IOException {

    NlsService nlsService = new NlsService(Locale.ENGLISH);

    URL mainViewUrl = getClass().getResource("main-view.fxml");
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

    tabPane = (TabPane) fxmlLoader.getNamespace().get("tabPane");
    mainTab = (Tab) fxmlLoader.getNamespace().get("mainTab");
    toolConfigTab = (Tab) fxmlLoader.getNamespace().get("toolConfigTab");
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
   * Polls the given condition until it becomes {@code true}, or fails with an {@link AssertionError} once {@code timeoutMillis} elapses. Useful for state that
   * is updated asynchronously (e.g. from a background thread).
   */
  public static void waitForCondition(Supplier<Boolean> condition, long timeoutMillis) throws InterruptedException {

    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < timeoutMillis) {
      if (condition.get()) {
        return;
      }
      Thread.sleep(100);
    }
    throw new AssertionError("Condition not met within timeout");
  }

  /**
   * Selects the given project and workspace, exactly like a user would, so that a test starts from a known context.
   */
  private void selectProjectAndWorkspace() {

    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));
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
    selectProjectAndWorkspace();

    // assert all IDE open buttons are enabled
    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled()).as(button.getId() + " button should be enabled when a workspace has been selected").isFalse();
    }
  }

  /**
   * Regression test for #2040: switching between projects must reset the workspace selection and keep the IDE open buttons and the context consistent.
   * <p>
   * Previously the workspace selection and IDE open buttons were not kept in sync when a different project was selected. This ensures that selecting a
   * new project clears the workspace selection and disables the IDE open buttons again, and that (re-)selecting the workspace of the new project
   * re-enables the buttons and points the context to the correct project.
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
   * This test ensures that the tool config tab is disabled before any workspace has been selected, and that its content is not eagerly loaded.
   */
  @Test
  public void testToolConfigTabDisabledWhenNoWorkspaceSelected() {

    assertThat(toolConfigTab.isDisabled()).as("toolConfigTab should be disabled before a workspace is selected").isTrue();
    assertThat(toolConfigTab.getContent()).as("toolConfigTab content should not be loaded before it has ever been selected").isNull();
  }

  /**
   * This test ensures that the tool config tab becomes enabled once a project and workspace have been selected.
   */
  @Test
  public void testToolConfigTabEnabledAfterWorkspaceSelected() {

    selectProjectAndWorkspace();

    assertThat(toolConfigTab.isDisabled()).as("toolConfigTab should be enabled once a workspace has been selected").isFalse();
  }

  /**
   * This test ensures that selecting the tool config tab lazily loads its content from tools-config.fxml.
   */
  @Test
  public void testSelectingToolConfigTabLoadsContent() throws InterruptedException {

    selectProjectAndWorkspace();
    interact(() -> tabPane.getSelectionModel().select(toolConfigTab));
    waitForCondition(() -> toolConfigTab.getContent() != null, 5000);

    Parent content = (Parent) toolConfigTab.getContent();
    assertThat(content.lookup("#toolsTree")).as("tools-config.fxml content should contain the tools tree").isNotNull();
    assertThat(content.lookup("#cancelButton")).as("tools-config.fxml content should contain the cancel button").isNotNull();
  }

  /**
   * This test ensures that the tools tree is populated with tool groups and tools as soon as the tool config tab content is loaded (the initial tree build
   * happens synchronously in {@code ToolSettingsController#initialize()}; only the per-tool edition/version lookups run on a background thread).
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testToolConfigTreePopulatedWithToolsAfterOpening() throws InterruptedException {

    selectProjectAndWorkspace();
    interact(() -> tabPane.getSelectionModel().select(toolConfigTab));
    waitForCondition(() -> toolConfigTab.getContent() != null, 5000);

    Parent content = (Parent) toolConfigTab.getContent();
    TreeView<ToolConfiguration> toolsTree = (TreeView<ToolConfiguration>) content.lookup("#toolsTree");
    waitForCondition(() -> toolsTree.getRoot() != null && !toolsTree.getRoot().getChildren().isEmpty(), 5000);

    List<TreeItem<ToolConfiguration>> groups = toolsTree.getRoot().getChildren();
    assertThat(groups).as("tools tree should contain at least one tool group").isNotEmpty();

    boolean hasToolLeaf = groups.stream().flatMap(group -> group.getChildren().stream()).anyMatch(item -> item.getValue() != null);
    assertThat(hasToolLeaf).as("tools tree should contain at least one tool leaf below a group").isTrue();
  }

  /**
   * This test ensures that the cancel button on the tool config tab navigates back to the main tab.
   */
  @Test
  public void testCancelButtonReturnsToMainTab() throws InterruptedException {

    selectProjectAndWorkspace();
    interact(() -> tabPane.getSelectionModel().select(toolConfigTab));
    waitForCondition(() -> toolConfigTab.getContent() != null, 5000);

    Parent content = (Parent) toolConfigTab.getContent();
    Button cancelButton = lookup(content, "#cancelButton");

    interact(cancelButton::fire);

    assertThat(tabPane.getSelectionModel().getSelectedItem()).as("cancel should navigate back to the main tab").isEqualTo(mainTab);
  }


  @SuppressWarnings("unchecked")
  private static <T> T lookup(Parent root, String selector) {

    return (T) root.lookup(selector);
  }
}
