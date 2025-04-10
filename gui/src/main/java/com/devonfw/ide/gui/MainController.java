package com.devonfw.ide.gui;

import java.io.File;
import java.nio.file.Path;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;

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
  private ScrollPane repositories;
  @FXML
  private ScrollPane ides;

  private String projectComboboxValue;
  private final String directoryPath = "C:\\projects\\";


  @FXML
  private void initialize() {

    updateProjectsComboBox(selectedProject);
  }

  @FXML
  private void toRepositories() {

    repositories.toFront();
  }

  @FXML
  private void toIDEs() {

    ides.toFront();
  }


  private void updateProjectsComboBox(ComboBox<String> projects) {

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

    projects.setOnAction(actionEvent -> projectComboboxValue = projects.getValue());
  }

  /**
   *
   */
  public void createNewProject() {

  }

  @FXML
  private void runProject() {

    final IdeLogListenerBuffer buffer = new IdeLogListenerBuffer();
    IdeLogLevel logLevel = IdeLogLevel.INFO;
    IdeStartContextImpl startContext = new IdeStartContextImpl(logLevel, level -> new IdeSubLoggerOut(level, null, true, logLevel, buffer));

    AbstractIdeGuiContext context = new AbstractIdeGuiContext(startContext, Path.of(directoryPath + projectComboboxValue));

    context.getCommandletManager().getCommandlet("intellij").run();
  }
}
