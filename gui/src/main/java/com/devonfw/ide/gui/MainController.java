package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.progress.GuiProgressBarHandling;
import com.devonfw.ide.gui.progress.TaskManager;
import com.devonfw.ide.gui.progress.TaskManager.ProgressListener;
import com.devonfw.ide.gui.progress.taskwindow.TaskOverviewWindow;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Controller of the main screen of the dashboard GUI.
 */
public class MainController implements ProgressListener {

  private static Logger LOG = LoggerFactory.getLogger(MainController.class);


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

    TaskManager.getInstance().addListener(this);
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

    selectedProject.getItems().clear();
    Path directory = Path.of(directoryPath);

    if (Files.exists(directory) && Files.isDirectory(directory)) {
      try (Stream<Path> subPaths = Files.list(directory)) {
        subPaths
            .filter(Files::isDirectory)
            .map(Path::getFileName)
            .map(Path::toString)
            .filter(name -> !name.startsWith("_"))
            .forEach(name -> selectedProject.getItems().add(name));
      } catch (IOException e) {
        throw new IllegalStateException("Failed to list projects!", e);
      }
    }

    selectedProject.setOnAction(actionEvent -> {

      projectValue = Path.of(selectedProject.getValue()).resolve(IdeContext.FOLDER_WORKSPACES);
      selectedWorkspace.setDisable(false);
      androidStudioOpen.setDisable(false);
      eclipseOpen.setDisable(false);
      intellijOpen.setDisable(false);
      vsCodeOpen.setDisable(false);
      selectedWorkspace.setValue("main");
      this.workspaceValue = Path.of("main");
      updateContext(selectedProject.getValue(), selectedWorkspace.getValue());
    });
  }

  @FXML
  private void setWorkspaceValue() {

    selectedWorkspace.getItems().clear();
    Path directory = Path.of(directoryPath).resolve(projectValue);
    if (Files.exists(directory) && Files.isDirectory(directory)) {
      try (Stream<Path> subPaths = Files.list(directory)) {
        subPaths
            .filter(Files::isDirectory)
            .map(Path::getFileName)
            .map(Path::toString)
            .forEach(name -> selectedWorkspace.getItems().add(name));

      } catch (IOException e) {
        throw new RuntimeException("Error occurred while fetching workspace names.", e);
      }
    }
    this.workspaceValue = Path.of(selectedWorkspace.getValue());
    updateContext(selectedProject.getValue(), selectedWorkspace.getValue());
  }

  private void openIDE(String inIde) {

    Task<Void> downloadTask = runIdeCommandTask(inIde);

    new Thread(downloadTask).start();
  }

  private static Task<Void> runIdeCommandTask(String inIde) {
    GuiProgressBarHandling task = (GuiProgressBarHandling) IdeGuiStateManager.getInstance().getCurrentContext()
        .newProgressBar("Starting " + inIde, 1, "Task", 1);
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

  @Override
  public void onProgressTaskUpdated(GuiProgressBarHandling task, long stepPosition) {

    LOG.info("Progress update at position {}", stepPosition);

    List<GuiProgressBarHandling> tasks = TaskManager.getInstance().getTasks();
    updateStatusLabel(tasks);
    if (tasks.size() == 1) {
      statusProgressBar.setProgress((double) task.getCurrentProgress() / task.getMaxSize());
    }
  }

  @Override
  public void onProgressTaskAdded(List<GuiProgressBarHandling> updatedTaskList) {

    updateStatusLabel(updatedTaskList);
  }

  @Override
  public void onProgressTaskRemoved(List<GuiProgressBarHandling> updatedTaskList) {

    updateStatusLabel(updatedTaskList);
  }

  private void updateStatusLabel(List<GuiProgressBarHandling> taskList) {

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
      GuiProgressBarHandling task = taskList.getFirst();
      statusLabel.setText(task.getTitle() + " [" + task.getCurrentProgress() + "/" + task.getMaxSize() + " " + task.getUnitName() + "]");
      statusLabel.setUnderline(false);
      statusLabel.setStyle("");

      statusProgressBar.setVisible(true);
      statusProgressBar.setPrefWidth(PROGRESSBAR_VISIBLE_WIDTH);
    } else {
      statusLabel.setText("IDEasy is ready.");
      statusProgressBar.setVisible(false);
      statusProgressBar.setPrefWidth(0);

      statusLabel.setUnderline(false);
      statusLabel.setStyle("");
    }
  }
}
