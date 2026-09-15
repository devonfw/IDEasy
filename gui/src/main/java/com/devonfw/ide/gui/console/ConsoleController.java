package com.devonfw.ide.gui.console;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Controller that manages an instance of a console using ListView for better performance with large outputs.
 */
public class ConsoleController {

  private final NlsService nlsService;

  /**
   * Thread-safe buffer collecting messages that arrive while a batched UI update is already pending. Keeps the FX thread from being overwhelmed by individual
   * {@link javafx.application.Platform#runLater()} submissions.
   */
  private final Deque<IdeLogEntry> outputBuffer = new ArrayDeque<>();

  /** Whether a batch flush is already scheduled on the FX thread. */
  private boolean flushPending;

  /** Observable list backing the ListView for efficient updates. */
  private final ObservableList<IdeLogEntry> logEntries = FXCollections.observableArrayList();

  @FXML
  private Button clearButton;

  @FXML
  private CheckBox autoScrollCheckBox;

  @FXML
  private Label consoleStatusLabel;

  @FXML
  private Label lineCountLabel;

  @FXML
  private ListView<IdeLogEntry> consoleListView;

  @FXML
  private void initialize() {
    setupListView();
    setupEventHandlers();
  }

  /// @param nlsService nlsService injection
  public ConsoleController(NlsService nlsService) {

    this.nlsService = nlsService;
  }

  /**
   * Sets up the ListView with a custom cell factory for colored log levels.
   */
  private void setupListView() {

    consoleListView.setItems(logEntries);
    consoleListView.setCellFactory(_ -> new LogEntryCell());
  }

  /**
   * Sets up event handlers for the buttons.
   */
  private void setupEventHandlers() {

    clearButton.setOnAction(_ -> clearConsole());
  }

  /**
   * Prints a simple message to the console.
   *
   * @param message message to be printed
   */
  public void appendOutput(String message) {

    appendOutput(new IdeLogEntry(IdeLogLevel.INFO, message));
  }

  /**
   * Prints a log entry to the console.
   *
   * @param entry the log entry to append
   */
  public void appendOutput(IdeLogEntry entry) {

    synchronized (outputBuffer) {
      outputBuffer.add(entry);
      if (!flushPending) {
        flushPending = true;
        FxHelper.runFxSafe(this::flushBuffer);
      }
    }
  }

  /**
   * Prints a message with a log level to the console.
   *
   * @param level the log level
   * @param message the message
   */
  public void appendOutput(IdeLogLevel level, String message) {

    appendOutput(new IdeLogEntry(level, message));
  }

  /**
   * Flushes all buffered messages to the console.
   */
  private void flushBuffer() {

    List<IdeLogEntry> entriesToAdd;
    synchronized (outputBuffer) {
      if (outputBuffer.isEmpty()) {
        flushPending = false;
        return;
      }
      entriesToAdd = outputBuffer.stream().toList();
      outputBuffer.clear();
      flushPending = false;
    }

    logEntries.addAll(entriesToAdd);

    if (autoScrollCheckBox.isSelected()) {
      scrollToBottom();
    }

    updateLineCount();
  }

  /**
   * Sets status text.
   *
   * @param status new status
   */
  public void setConsoleStatus(String status) {

    FxHelper.runFxSafe(() -> consoleStatusLabel.setText(status));
  }

  /**
   * Clears the console.
   */
  void clearConsole() {

    synchronized (outputBuffer) {
      outputBuffer.clear();
      flushPending = false;
    }

    logEntries.clear();
    updateLineCount();
    setConsoleStatus(nlsService.get("console_cleared"));
  }

  /**
   * Scrolls to the end of the console.
   */
  private void scrollToBottom() {

    if (!logEntries.isEmpty()) {
      consoleListView.scrollTo(logEntries.size() - 1);
    }
  }

  /**
   * Refreshes the line count.
   */
  private void updateLineCount() {

    if (nlsService != null) {
      FxHelper.runFxSafe(() -> lineCountLabel.setText(nlsService.get("console_line_count").replace("{0}", String.valueOf(logEntries.size()))));
    } else {
      FxHelper.runFxSafe(() -> lineCountLabel.setText("Lines: " + logEntries.size()));
    }
  }

  /**
   * Gets the current output that is on the console.
   *
   * @return a list of the current console output lines
   */
  public List<String> getConsoleOutputSnapshot() {

    return logEntries.stream().map(IdeLogEntry::displayValue).collect(Collectors.toList());
  }

  /**
   * Checks if auto-scroll is enabled.
   *
   * @return true if auto-scroll is enabled
   */
  public boolean isAutoScrollEnabled() {

    return autoScrollCheckBox.isSelected();
  }


  /**
   * Custom ListCell for rendering log entries with colors based on log level.
   */
  private static class LogEntryCell extends ListCell<IdeLogEntry> {

    private static final String BASE_STYLE = "-fx-font-family: 'Consolas', monospace; -fx-font-size: 11;";
    private static final String ERROR_STYLE = BASE_STYLE + " -fx-text-fill: #cc0000;";
    private static final String WARNING_STYLE = BASE_STYLE + " -fx-text-fill: #cc8800;";
    private static final String INFO_STYLE = BASE_STYLE + " -fx-text-fill: #000000;";
    private static final String DEBUG_STYLE = BASE_STYLE + " -fx-text-fill: #666666;";
    private static final String TRACE_STYLE = BASE_STYLE + " -fx-text-fill: #888888;";
    private static final String PLAIN_STYLE = BASE_STYLE + " -fx-text-fill: #333333;";
    private static final String ERROR_BG = "-fx-background-color: #fff0f0;";
    private static final String WARNING_BG = "-fx-background-color: #fff8e0;";

    @Override
    protected void updateItem(IdeLogEntry entry, boolean empty) {

      super.updateItem(entry, empty);
      if (empty || entry == null) {
        setText(null);
        setStyle(BASE_STYLE);
      } else {
        setText(entry.displayValue());
        setStyle(getStyleForEntry(entry));
      }
    }

    private String getStyleForEntry(IdeLogEntry entry) {

      if (entry.level() == null) {
        return PLAIN_STYLE;
      }
      return switch (entry.level()) {
        case ERROR -> ERROR_STYLE + " " + ERROR_BG;
        case WARNING -> WARNING_STYLE + " " + WARNING_BG;
        case INFO -> INFO_STYLE;
        case DEBUG -> DEBUG_STYLE;
        case TRACE -> TRACE_STYLE;
        default -> PLAIN_STYLE;
      };
    }
  }
}
