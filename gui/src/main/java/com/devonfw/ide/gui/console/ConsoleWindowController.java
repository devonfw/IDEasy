package com.devonfw.ide.gui.console;

import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
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
  private TextField consoleInput;

  @FXML
  private Button clearButton;

  @FXML
  private Button sendButton;

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

  private ConsoleOutputListener outputListener;
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
    sendButton.setOnAction(event -> sendInput());
    scrollToBottomButton.setOnAction(event -> scrollToBottom());
    hideButton.setOnAction(event -> hideConsole());

    // Enter-Taste im Input-Feld
    consoleInput.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        sendInput();
      }
    });
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
   * Sends input to the ConsoleHandler
   */
  private void sendInput() {
    String input = consoleInput.getText().trim();
    if (input.isEmpty()) {
      return;
    }

    // Eingabe in Konsole anzeigen
    appendOutput("> " + input);

    // Input leeren
    consoleInput.clear();

    // Listener benachrichtigen falls vorhanden
    if (outputListener != null) {
      outputListener.onInputSubmitted(input);
    }

    setStatus("Command sent");
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

  /**
   * Adds listener for output events
   *
   * @param listener the listener
   */
  public void setOutputListener(ConsoleOutputListener listener) {
    this.outputListener = listener;
  }

  /**
   * Interface for input events
   */
  @FunctionalInterface
  public interface ConsoleOutputListener {

    /**
     * Wird aufgerufen, wenn der Benutzer eine Eingabe sendet
     *
     * @param input die eingegebene Zeile
     */
    void onInputSubmitted(String input);
  }
}
