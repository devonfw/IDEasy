package com.devonfw.ide.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
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
  private Button statusCommandletButton;

  @FXML
  private SplitPane centerSplitPane;

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
    androidStudioOpen.setOnAction(event -> openAndroidStudio());
    eclipseOpen.setOnAction(event -> openEclipse());
    intellijOpen.setOnAction(event -> openIntellij());
    vsCodeOpen.setOnAction(event -> openVsCode());
    statusCommandletButton.setOnAction(event -> {
      runCommandlet("status");
    });

    centerSplitPane.getDividers().getFirst().positionProperty().addListener((obs, oldVal, newVal) -> {
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
      console.setVisible(false);
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
      console.setVisible(true);
      if (centerSplitPane.getDividers().get(0).getPosition() >= 0.9) {
        centerSplitPane.setDividerPosition(0, 0.75);
      }
      isConsoleVisible = true;
      LOG.debug("Console shown");
    }
  }
}
