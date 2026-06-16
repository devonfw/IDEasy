package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;
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
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;

/**
 * Controller of the main screen of the dashboard GUI.
 */
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

  /**
   * Constructor
   *
   * @param ideRoot the IDE_ROOT path
   * @param guiStateManager the {@link GuiStateManager} to be used in this application instance
   * @param taskManager the {@link TaskManager} to be used in this application instance
   */
  public MainController(String ideRoot, GuiStateManager guiStateManager, TaskManager taskManager) {

    LOG.debug("IDE_ROOT path={}", ideRoot);
    this.ideRootPath = ideRoot;
    this.taskManager = taskManager;
    this.guiStateManager = guiStateManager;
    this.projectManager = guiStateManager.getProjectManager();

    ListChangeListener<ProgressBarTask> taskListChangeListener = change -> {
      List<ProgressBarTask> tasks = taskManager.getTasks();

      while (change.next()) {
        if (change.wasAdded()) {
          LOG.debug("Added: {}", change.getAddedSubList());

          for (ProgressBarTask product : change.getAddedSubList()) {
            product.currentProgressProperty().addListener((obs, oldVal, newVal) ->
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
          task.close();
          return null;
        }
      };

      downloadTask.setOnFailed(_ -> Platform.runLater(() -> {
        task.close();
        IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, "Error occurred while launching " + inIde);
        errorDialog.showAndWait();
      }));
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
}
