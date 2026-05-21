package com.devonfw.ide.gui.console;

import java.io.IOException;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Controller für das Konsolen**fenster** zur Anzeige von (mehreren) Konsolen
 */
public class ConsolePaneController {

  private static final Logger LOG = Logger.getLogger(ConsolePaneController.class.getName());

  @FXML
  private TabPane consoleTabPane;

  @FXML
  public void initialize() {

  }

  /**
   * Opens a new console and returns its controller.
   *
   * @param name /title of the new console shown in the tab
   * @return the controller of the new console
   */
  public ConsoleController newConsole(String name) {
    Tab tab = new Tab(name);

    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("console.fxml"));

    try {
      tab.setContent(fxmlLoader.load());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    tab.setClosable(true);

    Platform.runLater(() -> {
      consoleTabPane.getTabs().add(tab);
      consoleTabPane.getSelectionModel().select(tab);
    });

    return fxmlLoader.getController();
  }
}
