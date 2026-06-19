package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.stream.Stream;
import javafx.concurrent.Task;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.control.ToggleButton;

import javafx.scene.layout.AnchorPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.console.ConsoleController;
import com.devonfw.ide.gui.context.GuiOutputListener;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiLogListener;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.OutputListener;
import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.modal.IdeDialog;

/**
 * Controller of the main screen of the dashboard GUI.
 */
public class MainController {

  private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

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
  private Button statusCommandletButton;

  @FXML
  private SplitPane centerSplitPane;
  private Divider centerDivider;

  @FXML
  private ToggleButton consolePaneToggleButton;

  @FXML
  private ConsoleController consoleController;

  @FXML
  private AnchorPane console;

  private IdeGuiLogListener guiLogListener;
  private OutputListener guiOutputListener;

  private final String directoryPath;
  private Path projectValue;
  private Path workspaceValue;

  /**
   * Constructor
   *
   * @param ideRootPath IDE_ROOT path.
   */
  public MainController(String directoryPath) {

    LOG.debug("IDE_ROOT path={}", directoryPath);
    this.directoryPath = directoryPath;

    this.projectManager = IdeGuiStateManager.getInstance().getProjectManager();
  }

  @FXML
  private void initialize() {

    setProjectsComboBox();
    consolePaneToggleButton.setOnAction(_ -> toggleConsole());
    androidStudioOpen.setOnAction(_ -> openAndroidStudio());
    eclipseOpen.setOnAction(_ -> openEclipse());
    intellijOpen.setOnAction(_ -> openIntellij());
    vsCodeOpen.setOnAction(_ -> openVsCode());
    statusCommandletButton.setOnAction(_ -> runCommandlet("status"));

    centerDivider = centerSplitPane.getDividers().getFirst();

    centerDivider.positionProperty().addListener((_, _, newVal) -> {
      //This is a bit of a weird behaviour in JavaFX, but even if you drag the divider fully down, the position value does not become 1, but something like 0.9935345
      consolePaneToggleButton.setSelected(newVal.doubleValue() < 0.99);
    });
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

    assert (directoryPath != null) : "directoryPath is null! Please check the setup of your environment variables (IDE_ROOT)";

    List<String> projects = projectManager.getProjectNames();

    selectedProject.getItems().clear();
    selectedProject.getItems().addAll(projects);

    selectedProject.setOnAction(_ -> {

      setWorkspaceComboBox();

      selectedWorkspace.setDisable(false);
    });
  }

  private void setWorkspaceComboBox() {

    List<String> workspaces = null;
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

  private void runCommandlet(String commandlet) {

    showConsole();

    this.guiLogListener = new IdeGuiLogListener(consoleController);
    this.guiOutputListener = new GuiOutputListener(consoleController);

    //TODO:update this since in PR for progress bars the handling of this will be implemented

    Task<Void> commandletTask = new Task<>() {

      @Override
      protected Void call() {

        try {
          IdeStartContextImpl startContext = new IdeStartContextImpl(IdeLogLevel.INFO, guiLogListener);
          IdeGuiContext context = new IdeGuiContext(startContext,
              Path.of(directoryPath).resolve(projectValue).resolve(workspaceValue));

          // Set output listener for process output
          context.setOutputListener(guiOutputListener);

          LOG.info("[GUI] === Running {} ===", commandlet);

          context.getCommandletManager().getCommandlet(commandlet).run();

          LOG.info("[GUI] === {} ran successfully. ===", commandlet);
        } catch (Exception e) {
          LOG.error("Failed to open {}", commandlet, e);
          consoleController.appendOutput("[ERROR] Failed to launch " + commandlet + ": " + e.getMessage());
          consoleController.setStatus("Error");
        }
        return null;
      }
    };

    Thread commandletThread = new Thread(commandletTask);
    commandletThread.setDaemon(true);
    commandletThread.start();
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
  
  private void updateContext(String selectedProjectName, String selectedWorkspaceName) {
    try {
      IdeGuiStateManager.getInstance().switchContext(selectedProjectName, selectedWorkspaceName);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
}
