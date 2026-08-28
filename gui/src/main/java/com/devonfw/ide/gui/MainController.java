package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.console.ConsoleController;
import com.devonfw.ide.gui.context.GuiOutputListener;
import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiLogListener;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.OutputListener;

/**
 * Controller of the main screen of the dashboard GUI.
 */
@SuppressWarnings("unused")
public class MainController {

  private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

  private final GuiStateManager guiStateManager;
  private final ProjectManager projectManager;
  private final TaskManager taskManager;

  private IdeGuiLogListener guiLogListener;
  private OutputListener guiOutputListener;

  private MainController oldMainController;

  private ListChangeListener<ProgressBarTask> taskListChangeListener;

  @FXML
  private ComboBox<String> selectedProject;

  @FXML
  private ComboBox<String> selectedWorkspace;

  @FXML
  private ComboBox<String> selectedLanguage;

  @FXML
  private Button androidStudioOpen;

  @FXML
  private Button eclipseOpen;

  @FXML
  private Button intellijOpen;

  @FXML
  private Button vsCodeOpen;

  @FXML
  private SplitPane centerSplitPane;

  @FXML
  private Divider centerDivider;

  @FXML
  private ToggleButton consolePaneToggleButton;

  @FXML
  private ConsoleController consoleController;

  @FXML
  private AnchorPane console;

  @FXML
  private Label statusLabel;

  @FXML
  private ProgressBar statusProgressBar;


  private final double PROGRESSBAR_VISIBLE_WIDTH = 150.0;

  private final String ideRootPath;

  private final Map<String, Locale> languageMap;

  private final NlsService nlsService;


  /**
   * Constructor
   *
   * @param ideRootPath the IDE_ROOT path
   * @param guiStateManager the {@link GuiStateManager} to be used in this application instance
   * @param nlsService nlsService instance
   * @param oldMainController the previous main controller whose GUI state (selections, console) is carried over on a re-load, or {@code null} on the first
   *     load
   */
  public MainController(String ideRootPath, GuiStateManager guiStateManager, NlsService nlsService, MainController oldMainController) {

    LOG.debug("IDE_ROOT path={}", ideRootPath);
    this.ideRootPath = ideRootPath;
    this.guiStateManager = guiStateManager;
    this.taskManager = guiStateManager.getTaskManager();
    this.projectManager = guiStateManager.getProjectManager();
    this.languageMap = new LinkedHashMap<>();
    this.nlsService = nlsService;
    this.oldMainController = oldMainController;

    setUpTaskListListener();
  }

  private void setUpTaskListListener() {

    this.taskListChangeListener = change -> {
      List<ProgressBarTask> tasks = taskManager.getTasks();

      while (change.next()) {
        if (change.wasAdded()) {
          LOG.debug("Added: {}", change.getAddedSubList());

          for (ProgressBarTask progressTask : change.getAddedSubList()) {
            progressTask.currentProgressProperty().addListener((_, _, _) ->
                updateStatusLabel(tasks)
            );
          }
          updateStatusLabel(tasks);
        } else if (change.wasRemoved()) {
          LOG.debug("Removed: {}", change.getRemoved());

          updateStatusLabel(tasks);
        } else if (change.wasUpdated()) {

          updateStatusLabel(tasks);
        }
      }
    };
    taskManager.getTasks().addListener(taskListChangeListener);
  }

  /** Guard to ensure {@link #initialize()} runs only once across all FXML files. */
  private boolean initialized;

  @FXML
  private void initialize() {

    if (this.initialized) {
      return;
    }

    setProjectsComboBox();
    initLanguageComboBox();
    selectedWorkspace.setOnAction(this::onWorkspaceSelected);
    consolePaneToggleButton.setOnAction(_ -> toggleConsole());

    centerDivider = centerSplitPane.getDividers().getFirst();
    centerDivider.positionProperty().addListener((_, _, newVal) -> {
      //This is a bit of a weird behaviour in JavaFX, but even if you drag the divider fully down,
      // the position value does not become 1, but something like 0.9935345
      consolePaneToggleButton.setSelected(newVal.doubleValue() < 0.99);
    });

    loadOldMainController();
    this.initialized = true;
  }

  private void onWorkspaceSelected(ActionEvent actionEvent) {

    String workspaceName = selectedWorkspace.getValue();
    if (workspaceName == null) {
      return;
    }
    updateContext(selectedProject.getValue(), workspaceName);
    setIdeButtonsDisabled(false);
  }

  private void loadOldMainController() {
    if (this.oldMainController == null) {
      return;
    }

    String oldProjectName = this.oldMainController.selectedProject.getValue();
    if (oldProjectName != null && selectedProject.getItems().contains(oldProjectName)) {
      selectedProject.setValue(oldProjectName);
      setWorkspaceComboBox();
      String oldWorkspaceName = this.oldMainController.selectedWorkspace.getValue();
      if (oldWorkspaceName != null) {
        selectedWorkspace.setValue(oldWorkspaceName);
        updateContext(oldProjectName, oldWorkspaceName);
      }
      selectedWorkspace.setDisable(this.oldMainController.selectedWorkspace.isDisable());
      setIdeButtonsDisabled(this.oldMainController.androidStudioOpen.isDisabled());
    }
    restoreConsoleState();

    this.oldMainController = null;
  }

  /**
   * Releases the resources of this controller. Called by the GUI after the view was replaced (e.g. on a language change) so that this controller no longer
   * observes the shared task list and can be garbage-collected.
   */
  public void dispose() {

    if (this.taskListChangeListener != null) {
      taskManager.getTasks().removeListener(this.taskListChangeListener);
      this.taskListChangeListener = null;
    }
    this.oldMainController = null;
  }

  /**
   * Restores the console state of the previous main view: its output, the auto-scroll preference and the pane's visibility.
   */
  private void restoreConsoleState() {

    ConsoleController oldConsoleController = this.oldMainController.consoleController;
    if (oldConsoleController == null) {
      return;
    }

    this.consoleController.setAutoScrollEnabled(oldConsoleController.isAutoScrollEnabled());

    this.consoleController.restoreOutput(oldConsoleController.getLogEntries());

    centerSplitPane.setDividerPosition(0, this.oldMainController.centerSplitPane.getDividers().getFirst().getPosition());
  }

  private void initLanguageComboBox() {

    this.languageMap.clear();
    selectedLanguage.getItems().clear();

    for (Locale locale : nlsService.getAvailableLocales()) {
      String displayName = nlsService.getLanguageDisplayName(locale);
      this.languageMap.put(displayName, locale);
    }

    selectedLanguage.getItems().addAll(this.languageMap.keySet());
    //initial value
    selectedLanguage.setValue(resolveLanguageSelection(nlsService.getLocale()));

    selectedLanguage.setOnAction(ev -> {
      String selection = selectedLanguage.getValue();
      Locale newLocale = this.languageMap.get(selection);
      if (newLocale != null) {
        nlsService.setLocale(newLocale);
      }
    });
  }

  private String resolveLanguageSelection(Locale currentLocale) {

    if (currentLocale == null) {
      return this.languageMap.keySet().stream().findFirst().orElse(null);
    }

    String languageTagMatch = null;
    String languageMatch = null;

    for (Map.Entry<String, Locale> entry : this.languageMap.entrySet()) {
      Locale entryLocale = entry.getValue();
      // Exact language tag match takes priority
      if (entryLocale.toLanguageTag().equalsIgnoreCase(currentLocale.toLanguageTag())) {
        return entry.getKey();
      }
      // Track language-only match as fallback
      if (languageMatch == null && entryLocale.getLanguage().equalsIgnoreCase(currentLocale.getLanguage())) {
        languageMatch = entry.getKey();
      }
    }

    // Return language-only match if found, otherwise first available
    return languageMatch != null ? languageMatch : this.languageMap.keySet().stream().findFirst().orElse(null);
  }


  @FXML
  private void openAndroidStudio() {

    runCommandlet("android-studio");
  }

  @FXML
  private void openEclipse() {

    runCommandlet("eclipse");
  }

  @FXML
  private void openIntellij() {

    runCommandlet("intellij");
  }

  @FXML
  private void openVsCode() {

    runCommandlet("vscode");
  }

  private void setProjectsComboBox() {

    assert (ideRootPath != null) : "directoryPath is null! Please check the setup of your environment variables (IDE_ROOT)";

    List<String> projects = projectManager.getProjectNames();

    selectedProject.getItems().clear();
    selectedProject.getItems().addAll(projects);

    selectedProject.setOnAction(_ -> {

      setWorkspaceComboBox();

      selectedWorkspace.setDisable(false);
    });
  }

  private void setWorkspaceComboBox() {

    List<String> workspaces;
    try {
      workspaces = projectManager.getWorkspaceNames(selectedProject.getValue());
    } catch (NotDirectoryException e) {
      throw new RuntimeException(e);
    }

    selectedWorkspace.setValue(null);
    selectedWorkspace.getItems().clear();
    selectedWorkspace.getItems().addAll(workspaces);

    if (workspaces.contains("main")) {
      selectedWorkspace.setValue("main");
      updateContext(selectedProject.getValue(), selectedWorkspace.getValue());
      setIdeButtonsDisabled(false);
    } else {
      setIdeButtonsDisabled(true);
    }
  }

  private void runCommandlet(String commandlet) {

    showConsole();
    Task<Void> commandletTask = runIdeCommandTask(commandlet);

    this.guiLogListener = new IdeGuiLogListener(consoleController);
    this.guiOutputListener = new GuiOutputListener(consoleController);

    Thread commandletThread = new Thread(commandletTask);
    commandletThread.setDaemon(true);
    commandletThread.start();
  }

  private Task<Void> runIdeCommandTask(String commandlet) {

    try (ProgressBarTask task = (ProgressBarTask) guiStateManager.getCurrentContext()
        .newProgressBarIndeterminate("Starting " + commandlet)) {
      Task<Void> downloadTask = new Task<>() {
        @Override
        protected Void call() {

          try {
            IdeStartContextImpl startContext = new IdeStartContextImpl(IdeLogLevel.INFO, guiLogListener);
            IdeGuiContext context = new IdeGuiContext(startContext,
                Path.of(ideRootPath).resolve(selectedProject.getValue()).resolve(selectedWorkspace.getValue()), taskManager);

            // Set output listener for process output
            context.setOutputListener(guiOutputListener);

            LOG.info("[GUI] === Running {} ===", commandlet);

            context.getCommandletManager().getCommandlet(commandlet).run();

            LOG.info("[GUI] === {} completed successfully. ===", commandlet);
          } catch (Exception e) {
            LOG.error("Failed to open {}", commandlet, e);
            consoleController.appendOutput("[ERROR] Failed to launch " + commandlet + ": " + e.getMessage());
          }
          return null;
        }
      };

      downloadTask.setOnFailed(_ -> Platform.runLater(() -> {
        task.close();
        IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, "Error occurred while launching " + commandlet);
        errorDialog.showAndWait();
      }));
      downloadTask.setOnSucceeded(_ -> Platform.runLater(task::close));
      return downloadTask;
    }
  }

  private void updateContext(String selectedProjectName, String selectedWorkspaceName) {

    try {
      guiStateManager.switchContext(selectedProjectName, selectedWorkspaceName);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }

  /**
   * Toggles the console visibility
   */
  public void toggleConsole() {

    if (centerSplitPane != null) {
      if (isConsoleVisible()) {
        hideConsole();
      } else {
        showConsole();
      }
      consolePaneToggleButton.setSelected(isConsoleVisible());
    }
  }

  /**
   * Hides the console panel
   */
  public void hideConsole() {

    if (centerSplitPane != null) {
      centerSplitPane.setDividerPosition(0, 1.0);
      LOG.debug("Console hidden");
    }
  }

  /**
   * Shows the console panel
   */
  public void showConsole() {

    if (centerSplitPane != null) {
      if (centerSplitPane.getDividers().getFirst().getPosition() >= 0.9) {
        centerSplitPane.setDividerPosition(0, 0.75);
      }
      LOG.debug("Console shown");
    }
  }

  private boolean isConsoleVisible() {
    return centerDivider.getPosition() <= 0.99 && console.isVisible();
  }

  private void updateStatusLabel(List<ProgressBarTask> taskList) {

    Platform.runLater(() -> {

      if (taskList.size() > 1) {
        statusLabel.setOnMouseClicked(e -> TaskOverviewWindow.getInstance(taskManager).showRelativeToReferenceNode(statusLabel));

        statusProgressBar.setVisible(false);
        statusProgressBar.setPrefWidth(0);
        statusLabel.setText(taskList.size() + " tasks running...");

        statusLabel.setUnderline(true);
        statusLabel.setStyle(
            "-fx-text-fill: blue;"
                + "-fx-cursor: hand"
        );
      } else if (taskList.size() == 1) {
        statusLabel.setOnMouseClicked(null);

        ProgressBarTask task = taskList.getFirst();
        statusLabel.setText(String.format(
            ProgressBarTask.TASK_DESCRIPTION_STRING_FORMAT,
            task.getTitle(),
            task.getCurrentProgress(),
            task.getMaxSize(),
            task.getUnitName())
        );
        statusLabel.setUnderline(false);
        statusLabel.setStyle("");

        statusProgressBar.setVisible(true);
        statusProgressBar.setPrefWidth(PROGRESSBAR_VISIBLE_WIDTH);
        statusProgressBar.setProgress((double) (task.getCurrentProgress()) / task.getMaxSize());
      } else {
        statusLabel.setOnMouseClicked(null);
        statusLabel.setText("IDEasy is ready.");
        statusProgressBar.setVisible(false);
        statusProgressBar.setPrefWidth(0);

        statusLabel.setUnderline(false);
        statusLabel.setStyle("");
      }
    });
  }

  private void setIdeButtonsDisabled(boolean disabled) {

    this.androidStudioOpen.setDisable(disabled);
    this.eclipseOpen.setDisable(disabled);
    this.intellijOpen.setDisable(disabled);
    this.vsCodeOpen.setDisable(disabled);
  }
}
