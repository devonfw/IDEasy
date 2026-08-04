package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.io.IOException;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.progress.ProgressBarTask;
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
  private BorderPane rootPane;

  @FXML
  private StackPane centerPane;

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
  private Button commandletOpen;

  private final String directoryPath;
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

    ListChangeListener<ProgressBarTask> taskListChangeListener = change -> {
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
      commandletOpen.setDisable(false);
    });
  }

  private void openIDE(String inIde) {

    Task<Void> downloadTask = runIdeCommandTask(inIde);

    new Thread(downloadTask).start();
  }

  private Task<Void> runIdeCommandTask(String inIde) {

    try (ProgressBarTask task = (ProgressBarTask) guiStateManager.getCurrentContext()
        .newProgressBarIndeterminate("Starting " + inIde)) {
      Task<Void> downloadTask = new Task<>() {
        @Override
        protected Void call() {
          guiStateManager
              .getCurrentContext()
              .getCommandletManager()
              .getCommandlet(inIde)
              .run();
          return null;
        }
      };

      downloadTask.setOnFailed(_ -> Platform.runLater(() -> {
        task.close();
        IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, "Error occurred while launching " + inIde);
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

  @FXML
  private void openCommandlet() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("commandlet-view.fxml"));
      loader.setResources(this.nlsService.getResourceBundle());
      loader.setController(new CommandletController(guiStateManager.getCurrentContext()));
      Parent root = loader.load();

      Stage stage = (Stage) selectedProject.getScene().getWindow();
      stage.setScene(new Scene(root));
    } catch (IOException e) {
      LOG.error("Failed to load commandlet view", e);
      new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage()).showAndWait();
    }
  }


  @FXML
  private void openCommandlet() {
    Runnable goBack = () -> rootPane.setCenter(centerPane);
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("commandlet-view.fxml"));
      loader.setResources(this.nlsService.getResourceBundle());
      loader.setController(new CommandletController(guiStateManager.getCurrentContext(), goBack));
      Parent commandletView = loader.load();

      rootPane.setCenter(commandletView);
    } catch (IOException e) {
      LOG.error("Failed to load commandlet view", e);
      new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage()).showAndWait();
    }
  }

}
