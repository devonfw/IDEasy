package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Controller of the main screen of the dashboard GUI.
 */
public class MainController {

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

  private final String directoryPath;
  private Path projectValue;
  private Path workspaceValue;

  /**
   * Constructor
   */
  public MainController(String directoryPath) {
    LOG.debug("IDE_ROOT path={}", directoryPath);
    this.directoryPath = directoryPath;
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

    IdeGuiStateManager
        .getInstance()
        .getCurrentContext()
        .getCommandletManager()
        .getCommandlet(inIde)
        .run();
  }

  private void updateContext(String selectedProjectName, String selectedWorkspaceName) {
    try {
      IdeGuiStateManager.getInstance().switchContext(selectedProjectName, selectedWorkspaceName);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }
}
