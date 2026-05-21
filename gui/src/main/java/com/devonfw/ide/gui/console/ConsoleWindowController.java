package com.devonfw.ide.gui.console;

import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

import com.devonfw.ide.gui.MainController;

/**
 * Controller für das Konsolenfenster zur Anzeige von Terminal-Ausgaben und Eingabe
 */
public class ConsoleWindowController {

  private static final Logger LOG = Logger.getLogger(ConsoleWindowController.class.getName());

  @FXML
  private AnchorPane consolePaneRoot;

  @FXML
  private TextArea consoleOutput;

  @FXML
  private Button clearButton;

  @FXML
  private Button scrollToBottomButton;

  @FXML
  private Button hideButton;

  @FXML
  private CheckBox autoScrollCheckBox;

  @FXML
  private ScrollPane outputScrollPane;

  @FXML
  private Label statusLabel;

  @FXML
  private Label lineCountLabel;

  private MainController mainController;

  @FXML
  public void initialize() {
    setupEventHandlers();
    setupTextAreaBindings();
  }

  /**
   * Richtet die Event-Handler für Buttons und Eingabefeld ein
   */
  private void setupEventHandlers() {

    clearButton.setOnAction(event -> clearConsole());
    scrollToBottomButton.setOnAction(event -> scrollToBottom());
    hideButton.setOnAction(event -> hideConsole());
  }

  /**
   * Richtet die Bindings für TextArea-Änderungen ein
   */
  private void setupTextAreaBindings() {
    // Listener für TextArea-Änderungen
    consoleOutput.textProperty().addListener((observable, oldValue, newValue) -> {
      // Zeilenanzahl aktualisieren
      updateLineCount();

      // Auto-scroll wenn aktiviert
      if (autoScrollCheckBox.isSelected()) {
        scrollToBottom();
      }
    });
  }

  /**
   * Gibt eine Nachricht zur Konsole aus
   *
   * @param message die auszugebende Nachricht
   */
  public void appendOutput(String message) {
    Platform.runLater(() -> {
      if (consoleOutput.getText().isEmpty()) {
        consoleOutput.setText(message);
      } else {
        consoleOutput.appendText("\n" + message);
      }
    });
  }

  /**
   * prints a string to the screen with new line
   *
   * @param message string to be printed
   */
  public void appendOutputLine(String message) {
    appendOutput(message);
  }

  /**
   * sets status text
   *
   * @param status new status
   */
  public void setStatus(String status) {
    Platform.runLater(() -> statusLabel.setText(status));
  }

  /**
   * Clears the console
   */
  private void clearConsole() {
    consoleOutput.clear();
    updateLineCount();
    setStatus("Console cleared");
  }

  /**
   * Scrolls to the end of the console
   */
  private void scrollToBottom() {
    Platform.runLater(() -> outputScrollPane.setVvalue(1.0));
  }

  /**
   * Hides the console panel using the MainController
   */
  private void hideConsole() {
    if (mainController != null) {
      mainController.hideConsole();
    } else {
      LOG.warning("MainController not set, cannot hide console");
    }
  }

  /**
   * Refreshes the line count
   */
  private void updateLineCount() {
    int lines = consoleOutput.getText().split("\n", -1).length;
    Platform.runLater(() -> lineCountLabel.setText("Lines: " + lines));
  }

  /**
   * Sets the MainController for console toggle functionality
   *
   * @param controller the MainController
   */
  public void setMainController(MainController controller) {
    this.mainController = controller;
  }
}
