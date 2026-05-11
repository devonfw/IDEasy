package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.TaskManager;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;

/**
 * Controller of the main screen of the dashboard GUI.
 */
public class MainController {

  private static Logger LOG = LoggerFactory.getLogger(MainController.class);

  private ProjectManager projectManager;


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

  private final String directoryPath;
  private Path projectValue;
  private Path workspaceValue;

  /**
   * Constructor
   */
  public MainController(String directoryPath) {

    LOG.debug("IDE_ROOT path={}", directoryPath);
    this.directoryPath = directoryPath;

    ListChangeListener<ProgressBarTask> taskListChangeListener = change -> {
      List<ProgressBarTask> tasks = TaskManager.getInstance().getTasks();

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
    TaskManager.getInstance().getTasks().addListener(taskListChangeListener);

    this.projectManager = IdeGuiStateManager.getInstance().getProjectManager();
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

    assert (directoryPath != null) : "directoryPath is null! Please check the setup of your environment variables (IDE_ROOT)";

    List<String> projects = projectManager.getProjectNames();

    selectedProject.getItems().clear();
    selectedProject.getItems().addAll(projects);

    selectedProject.setOnAction(actionEvent -> {

      setWorkspaceComboBox();

      selectedWorkspace.setDisable(false);
    });
  }

  private void setWorkspaceComboBox() {

    List<String> workspaces = projectManager.getWorkspaceNames(selectedProject.getValue());

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

  private static Task<Void> runIdeCommandTask(String inIde) {

    ProgressBarTask task = (ProgressBarTask) IdeGuiStateManager.getInstance().getCurrentContext()
        .newProgressBarIndeterminate("Starting " + inIde);
    Task<Void> downloadTask = new Task<>() {
      @Override
      protected Void call() {
        IdeGuiStateManager
            .getInstance()
            .getCurrentContext()
            .getCommandletManager()
            .getCommandlet(inIde)
            .run();
        TaskManager.getInstance().removeTask(task);
        return null;
      }
    };

    downloadTask.setOnFailed(e -> {
      Platform.runLater(() -> {
        IdeDialog errorDialog = new IdeDialog(IdeDialog.AlertType.ERROR, "Error occurred while launching " + inIde);
        errorDialog.showAndWait();
      });
    });
    return downloadTask;
  }

  private void updateContext(String selectedProjectName, String selectedWorkspaceName) {

    try {
      IdeGuiStateManager.getInstance().switchContext(selectedProjectName, selectedWorkspaceName);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }

  //TODO: remove after testing
  public void addTaskTest() {

    LOG.error("Adding task");
    IdeGuiStateManager.getInstance()
        .getCurrentContext()
        .newProgressbarForExtracting(1024);

    IdeGuiStateManager.getInstance()
        .getCurrentContext()
        .newProgressbarForCopying(1024);

    IdeGuiStateManager.getInstance()
        .getCurrentContext()
        .newProgressBarForDownload(1024);

    IdeGuiStateManager.getInstance()
        .getCurrentContext()
        .newProgressBarForPlugins(3);
  }

  //TODO: remove after testing
  public void removeTaskTest() {

    TaskManager.getInstance().removeTask(TaskManager.getInstance().getTasks().getFirst());
  }

  private void updateStatusLabel(List<ProgressBarTask> taskList) {

    Platform.runLater(() -> {
      statusLabel.setOnMouseClicked(e -> TaskOverviewWindow.getInstance(statusLabel).show());

      if (taskList.size() > 1) {
        statusProgressBar.setVisible(false);
        statusProgressBar.setPrefWidth(0);
        statusLabel.setText(taskList.size() + " tasks running...");

        statusLabel.setUnderline(true);
        statusLabel.setStyle(
            "-fx-text-fill: blue;"
                + "-fx-cursor: hand"
        );
      } else if (taskList.size() == 1) {
        ProgressBarTask task = taskList.getFirst();
        statusLabel.setText(task.getTitle() + " [" + task.getCurrentProgress() + "/" + task.getMaxSize() + " " + task.getUnitName() + "]");
        statusLabel.setUnderline(false);
        statusLabel.setStyle("");

        statusProgressBar.setVisible(true);
        statusProgressBar.setPrefWidth(PROGRESSBAR_VISIBLE_WIDTH);
        statusProgressBar.setProgress((double) (task.getCurrentProgress()) / task.getMaxSize());
      } else {
        statusLabel.setText("IDEasy is ready.");
        statusProgressBar.setVisible(false);
        statusProgressBar.setPrefWidth(0);

        statusLabel.setUnderline(false);
        statusLabel.setStyle("");
      }
    });
  }
}
