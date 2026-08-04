package com.devonfw.ide.gui.console;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;

import com.devonfw.ide.gui.FxHelper;

/**
 * Controller that manages an instance if a console.
 */
public class ConsoleController {

  /**
   * Thread-safe buffer collecting messages that arrive while a batched UI update is already pending. Keeps the FX thread from being overwhelmed by individual
   * {@link Platform#runLater()} submissions.
   */
  private final Deque<String> outputBuffer = new ArrayDeque<>();

  /** Whether a batch flush is already scheduled on the FX thread. */
  private boolean flushPending;

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

  @FXML
  private void initialize() {
    setupEventHandlers();
  }

  /**
   * Sets up event handlers for the buttons.
   */
  private void setupEventHandlers() {

    clearButton.setOnAction(_ -> clearConsole());
  }

  /**
   * Prints a message to the console.
   * <p>
   * Messages are buffered and flushed in a single FX-thread operation to prevent the FX thread from being overwhelmed when many messages arrive concurrently
   * (e.g. during IDE startup).
   *
   * @param message message to be printed
   */
  public void appendOutput(String message) {
    synchronized (outputBuffer) {
      outputBuffer.add(message);
      if (!flushPending) {
        flushPending = true;
        FxHelper.runFxSafe(this::flushBuffer);
      }
    }
  }

  /**
   * Flushes all buffered messages to the console in a single FX-thread operation.
   */
  private void flushBuffer() {
    String text;
    synchronized (outputBuffer) {
      if (outputBuffer.isEmpty()) {
        flushPending = false;
        return;
      }
      StringBuilder sb = new StringBuilder();
      while (!outputBuffer.isEmpty()) {
        sb.append(outputBuffer.pollFirst()).append('\n');
      }
      text = sb.toString();
      flushPending = false;
    }

    consoleOutput.appendText(text);

    if (autoScrollCheckBox.isSelected()) {
      scrollToBottom();
    }

    updateLineCount();
  }

  /**
   * sets status text
   *
   * @param status new status
   */
  public void setStatus(String status) {

    FxHelper.runFxSafe(() -> statusLabel.setText(status));
  }

  /**
   * Clears the console
   */
  void clearConsole() {
    synchronized (outputBuffer) {
      outputBuffer.clear();
      flushPending = false;
    }

    consoleOutput.clear();
    updateLineCount();
    setStatus("Console cleared");
  }

  /**
   * Scrolls to the end of the console.
   * <p>
   * Uses {@link TextArea#positionCaret(int)} to scroll to the end of the text. This works because TextArea
   * scrolls to make its content visible via {@link Node#scrollTo()}, which operates against visual bounds —
   * unlike {@link ScrollPane#setVvalue(double)} which depends on the ScrollPane's lazy vmax computation.
   * </p>
   */
  private void scrollToBottom() {

    int length = consoleOutput.getLength();
    consoleOutput.positionCaret(length);
  }

  /**
   * Refreshes the line count
   */
  private void updateLineCount() {

    int lines = consoleOutput.getText().split("\n", -1).length;
    FxHelper.runFxSafe(() -> lineCountLabel.setText("Lines: " + lines));
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
