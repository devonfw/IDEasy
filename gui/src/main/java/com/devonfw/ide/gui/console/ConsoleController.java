package com.devonfw.ide.gui.console;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller that manages an instance if a console.
 */
public class ConsoleController {

  @FXML
  private Button clearButton;

  @FXML
  private Button scrollToBottomButton;

  @FXML
  private Button hideButton;

  @FXML
  private CheckBox autoScrollCheckBox;

  @FXML
  private Label statusLabel;

  @FXML
  private Label lineCountLabel;

  @FXML
  private ScrollPane outputScrollPane;

  @FXML
  private TextArea consoleOutput;

  private static final Logger LOG = LoggerFactory.getLogger(ConsoleController.class);

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
  }

  /**
   * Richtet die Bindings für TextArea-Änderungen ein
   */
  private void setupTextAreaBindings() {

    consoleOutput.textProperty().addListener((observable, oldValue, newValue) -> {

      updateLineCount();

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

    Platform.runLater(() -> consoleOutput.appendText(message + "\n"));
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
   * Refreshes the line count
   */
  private void updateLineCount() {

    int lines = consoleOutput.getText().split("\n", -1).length;
    Platform.runLater(() -> lineCountLabel.setText("Lines: " + lines));
  }
}
