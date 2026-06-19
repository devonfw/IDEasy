package com.devonfw.ide.gui.console;

import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;

/**
 * Controller that manages an instance if a console.
 */
public class ConsoleController {

  @FXML
  private Button clearButton;

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

  /**
   *
   */
  @FXML
  private void initialize() {

    setupEventHandlers();
    setupTextAreaBindings();
  }

  /**
   * Richtet die Event-Handler für Buttons und Eingabefeld ein
   */
  private void setupEventHandlers() {

    clearButton.setOnAction(_ -> clearConsole());
  }

  /**
   * Richtet die Bindings für TextArea-Änderungen ein
   */
  private void setupTextAreaBindings() {

    consoleOutput.textProperty().addListener((_, _, _) -> {

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
  void clearConsole() {

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

  /**
   * Get the current output that is on the console.
   *
   * @return a list of the current console output lines
   */
  public List<String> getConsoleOutputSnapshot() {
    return Arrays.stream(consoleOutput.getText().split("\n", -1)).filter(line -> !line.isEmpty()).toList();
  }
}
