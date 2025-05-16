package com.devonfw.ide.gui;

import java.io.File;
import java.nio.file.Path;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;
import com.devonfw.tools.ide.log.IdeSubLoggerOut;

/**
 *
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

  private final String directoryPath = "C:\\projects\\";
  private String projectValue;
  private String workspaceValue;


  @FXML
  private void initialize() {

    setProjectsComboBox(selectedProject);
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


  private void setProjectsComboBox(ComboBox<String> projects) {

    projects.getItems().clear();
    File directory = new File(directoryPath);
    if (directory.exists() && directory.isDirectory()) {

      File[] subDirectories = directory.listFiles(File::isDirectory);
      if (subDirectories != null) {

        for (File subDirectory : subDirectories) {

          String name = subDirectory.getName();
          if (!name.startsWith("_")) {

            projects.getItems().add(name);
          }
        }
      }
    }

    projects.setOnAction(actionEvent -> {

      projectValue = projects.getValue() + "\\workspaces";
      setWorkspaceValue(selectedWorkspace);
      selectedWorkspace.setDisable(false);
      androidStudioOpen.setDisable(false);
      eclipseOpen.setDisable(false);
      intellijOpen.setDisable(false);
      vsCodeOpen.setDisable(false);
    });
  }

  private void setWorkspaceValue(ComboBox<String> workspace) {

    workspace.getItems().clear();
    File directory = new File(directoryPath + projectValue);
    if (directory.exists() && directory.isDirectory()) {

      File[] subDirectories = directory.listFiles(File::isDirectory);
      if (subDirectories != null) {

        for (File subDirectory : subDirectories) {

          String name = subDirectory.getName();
          workspace.getItems().add(name);
          workspace.setValue("main");
        }
      }
    }

    workspace.setOnAction(actionEvent -> {

      workspaceValue = workspace.getValue();
    });
  }

  private void openIDE(String inIde) {

    final IdeLogListenerBuffer buffer = new IdeLogListenerBuffer();
    IdeLogLevel logLevel = IdeLogLevel.INFO;
    IdeStartContextImpl startContext = new IdeStartContextImpl(logLevel, level -> new IdeSubLoggerOut(level, null, true, logLevel, buffer));
    IdeGuiContext context = new IdeGuiContext(startContext, Path.of(directoryPath + projectValue + workspaceValue));
    context.getCommandletManager().getCommandlet(inIde).run();
  }
}
