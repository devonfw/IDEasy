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
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.progress.GuiTask;
import com.devonfw.ide.gui.progress.TaskState;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;

/**
 * Controller of the main screen of the dashboard GUI.
 */
@SuppressWarnings("unused")
public class MainController {

  private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

  private final GuiStateManager guiStateManager;
  private final ProjectManager projectManager;
  private final TaskManager taskManager;


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
  private Label statusLabel;

  @FXML
  private ProgressBar statusProgressBar;
  private final double PROGRESSBAR_VISIBLE_WIDTH = 150.0;

  private final String ideRootPath;
  private Path projectValue;
  private Path workspaceValue;

  private final Map<String, Locale> languageMap;

  private final NlsService nlsService;


  /**
   * Constructor
   *
   * @param ideRoot the IDE_ROOT path
   * @param guiStateManager the {@link GuiStateManager} to be used in this application instance
   */
  public MainController(String ideRoot, GuiStateManager guiStateManager, NlsService nlsService) {

    LOG.debug("IDE_ROOT path={}", ideRoot);
    this.ideRootPath = ideRoot;
    this.guiStateManager = guiStateManager;
    this.taskManager = guiStateManager.getTaskManager();
    this.projectManager = guiStateManager.getProjectManager();
    this.languageMap = new LinkedHashMap<>();
    this.nlsService = nlsService;

    setUpTaskListListener();
  }

  private void setUpTaskListListener() {

    // The task list is created with an extractor, so additions, removals and changes to a task's own properties all arrive here.
    ListChangeListener<GuiTask> taskListChangeListener = change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          LOG.debug("Added: {}", change.getAddedSubList());
        } else if (change.wasRemoved()) {
          LOG.debug("Removed: {}", change.getRemoved());
        }
      }
      updateStatusLabel(taskManager.getTasks());
    };
    taskManager.getTasks().addListener(taskListChangeListener);
  }

  @FXML
  private void initialize() {

    setProjectsComboBox();
    initLanguageComboBox();
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

    openIDE("android-studio");
  }

  @FXML
  private void openEclipse() {

    openIDE("eclipse");
  }

  @FXML
  private void openIntellij() {

    openIDE("intellij");
  }

  @FXML
  private void openVsCode() {

    openIDE("vscode");
  }

  private void setProjectsComboBox() {

    assert (ideRootPath != null) : "directoryPath is null! Please check the setup of your environment variables (IDE_ROOT)";

    List<String> projects = projectManager.getProjectNames();

    selectedProject.getItems().clear();
    selectedProject.getItems().addAll(projects);

    selectedProject.setOnAction(actionEvent -> {

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

    selectedWorkspace.getItems().clear();
    selectedWorkspace.getItems().addAll(workspaces);

    selectedWorkspace.setOnAction(actionEvent -> {
      updateContext(selectedProject.getValue(), selectedWorkspace.getValue());

      androidStudioOpen.setDisable(false);
      eclipseOpen.setDisable(false);
      intellijOpen.setDisable(false);
      vsCodeOpen.setDisable(false);
    });
  }

  private void openIDE(String inIde) {

    Task<Void> downloadTask = runIdeCommandTask(inIde);

    new Thread(downloadTask).start();
  }

  private Task<Void> runIdeCommandTask(String inIde) {

    Task<Void> downloadTask = new Task<>() {
      @Override
      protected Void call() {
        // Each execution gets its own context so that concurrently started commands keep independent step stacks.
        IdeGuiContext runContext = guiStateManager.newRunContext();
        // Wrapping the commandlet in a root step gives the user a single task that reports on all steps the commandlet creates below it.
        runContext.newStep("Starting " + inIde).run(() ->
            runContext.getCommandletManager().getCommandlet(inIde).run());
        return null;
      }
    };

    downloadTask.setOnFailed(_ -> Platform.runLater(() -> {
      IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, "Error occurred while launching " + inIde);
      errorDialog.showAndWait();
    }));
    return downloadTask;
  }

  private void updateContext(String selectedProjectName, String selectedWorkspaceName) {

    try {
      guiStateManager.switchContext(selectedProjectName, selectedWorkspaceName);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }

  private void updateStatusLabel(List<GuiTask> taskList) {

    // runFxSafe rather than runLater: the task list notifies us on the FX thread already, so queueing another hop per progress update only adds churn.
    FxHelper.runFxSafe(() -> {
      // Finished steps stay in the list until the user dismisses them, so the status bar reports on the running ones.
      List<GuiTask> runningTasks = taskList.stream().filter(GuiTask::isRunning).toList();

      if (runningTasks.size() > 1) {
        showLinkStatus(runningTasks.size() + " tasks running...");
      } else if (runningTasks.size() == 1) {
        showSingleTaskStatus(runningTasks.getFirst());
      } else if (!taskList.isEmpty()) {
        showLinkStatus(buildFinishedSummary(taskList));
      } else {
        showIdleStatus();
      }
    });
  }

  private String buildFinishedSummary(List<GuiTask> taskList) {

    long failed = taskList.stream().filter(task -> task.getState() == TaskState.FAILED).count();
    if (failed > 0) {
      return String.format("%d of %d tasks failed", failed, taskList.size());
    }
    return String.format("%d tasks finished", taskList.size());
  }

  /**
   * Shows a clickable status that opens the {@link TaskOverviewWindow}, used whenever a single task cannot represent the state.
   */
  private void showLinkStatus(String text) {

    statusLabel.setOnMouseClicked(_ -> TaskOverviewWindow.getInstance(taskManager).showRelativeToReferenceNode(statusLabel));
    statusLabel.setText(text);
    statusLabel.setUnderline(true);
    statusLabel.setStyle("-fx-text-fill: blue;-fx-cursor: hand");

    statusProgressBar.setVisible(false);
    statusProgressBar.setPrefWidth(0);
  }

  private void showSingleTaskStatus(GuiTask task) {

    statusLabel.setOnMouseClicked(_ -> TaskOverviewWindow.getInstance(taskManager).showRelativeToReferenceNode(statusLabel));
    statusLabel.setText(task.displayTextProperty().get());
    statusLabel.setUnderline(true);
    statusLabel.setStyle("-fx-text-fill: blue;-fx-cursor: hand");

    statusProgressBar.setVisible(true);
    statusProgressBar.setPrefWidth(PROGRESSBAR_VISIBLE_WIDTH);
    // a value of GuiTaskModel.INDETERMINATE maps directly onto the indeterminate animation of the JavaFX progress bar.
    statusProgressBar.setProgress(task.progressProperty().get());
  }

  private void showIdleStatus() {

    statusLabel.setOnMouseClicked(null);
    statusLabel.setText("IDEasy is ready.");
    statusLabel.setUnderline(false);
    statusLabel.setStyle("");

    statusProgressBar.setVisible(false);
    statusProgressBar.setPrefWidth(0);
  }
}
