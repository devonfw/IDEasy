package com.devonfw.ide.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.console.ConsoleController;
import com.devonfw.ide.gui.console.ConsolePaneController;
import com.devonfw.ide.gui.context.GuiOutputListener;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiLogListener;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.OutputListener;

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

  @FXML
  private SplitPane centerSplitPane;

  @FXML
  private ToggleButton consolePaneToggleButton;

  @FXML
  private ConsolePaneController consolePaneController;

  private IdeGuiLogListener guiLogListener;
  private OutputListener guiOutputListener;

  private final String directoryPath;
  private Path projectValue;
  private Path workspaceValue;
  private boolean isConsoleVisible = true;
  private double lastDividerPosition = 0.75;

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
    consolePaneToggleButton.setOnAction(event -> toggleConsole());
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
  }

  private void openIDE(String inIde) {

    showConsole();

    ConsoleController consoleController = consolePaneController.newConsole("Running " + inIde);

    this.guiLogListener = new IdeGuiLogListener(consoleController);
    this.guiOutputListener = new GuiOutputListener(consoleController);

    //TODO:update this since in PR for progress bars the handling of this will be implemented

    new Thread(() -> {
      try {
        IdeStartContextImpl startContext = new IdeStartContextImpl(IdeLogLevel.INFO, guiLogListener);
        IdeGuiContext context = new IdeGuiContext(startContext,
            Path.of(this.directoryPath).resolve(this.projectValue).resolve(this.workspaceValue));

        // Set output listener for process output
        context.setOutputListener(guiOutputListener);

        context.getCommandletManager().getCommandlet(inIde).run();

        consoleController.appendOutput(inIde + "started successfully.");
      } catch (Exception e) {
        LOG.error("Failed to open {}", inIde, e);
        consoleController.appendOutput("[ERROR] Failed to launch " + inIde + ": " + e.getMessage());
        consoleController.setStatus("Error");

      }
    }).start();
  }

  /**
   * Toggles the console visibility
   */
  public void toggleConsole() {
    if (centerSplitPane != null) {
      if (isConsoleVisible) {
        hideConsole();
      } else {
        showConsole();
      }
      consolePaneToggleButton.setSelected(isConsoleVisible);
    }
  }

  /**
   * Hides the console panel
   */
  public void hideConsole() {
    if (centerSplitPane != null) {
      lastDividerPosition = centerSplitPane.getDividers().get(0).getPosition();
      centerSplitPane.setDividerPosition(0, 1.0);
      isConsoleVisible = false;
      LOG.debug("Console hidden");
    }
  }

  /**
   * Shows the console panel
   */
  public void showConsole() {
    if (centerSplitPane != null) {
      if (lastDividerPosition >= 0.9) {
        centerSplitPane.setDividerPosition(0, 0.75);
      } else {
        centerSplitPane.setDividerPosition(0, lastDividerPosition);
      }
      isConsoleVisible = true;
      LOG.debug("Console shown");
    }
  }
}
