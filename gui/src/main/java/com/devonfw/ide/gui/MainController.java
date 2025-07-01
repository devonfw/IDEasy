package com.devonfw.ide.gui;

import static ch.qos.logback.core.util.OptionHelper.getEnv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;
import com.devonfw.tools.ide.log.IdeSubLoggerOut;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Controller of the main screen of the dashboard GUI.
 */
public class MainController {

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

  private final String directoryPath = getEnv(IdeVariables.IDE_ROOT.getName());
  private Path projectValue;
  private Path workspaceValue;


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
      setWorkspaceValue();
      selectedWorkspace.setDisable(false);
      androidStudioOpen.setDisable(false);
      eclipseOpen.setDisable(false);
      intellijOpen.setDisable(false);
      vsCodeOpen.setDisable(false);
    });
  }

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

        selectedWorkspace.setValue("main");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    selectedWorkspace.setOnAction(actionEvent -> workspaceValue = Path.of(selectedWorkspace.getValue()));
  }

  private void openIDE(String inIde) {

    final IdeLogListenerBuffer buffer = new IdeLogListenerBuffer();
    IdeLogLevel logLevel = IdeLogLevel.INFO;
    IdeStartContextImpl startContext = new IdeStartContextImpl(logLevel, level -> new IdeSubLoggerOut(level, null, true, logLevel, buffer));
    IdeGuiContext context = new IdeGuiContext(startContext, Path.of(directoryPath).resolve(projectValue).resolve(workspaceValue));
    context.getCommandletManager().getCommandlet(inIde).run();
  }
}
