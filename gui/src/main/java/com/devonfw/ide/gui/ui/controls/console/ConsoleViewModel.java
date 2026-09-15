package com.devonfw.ide.gui.ui.controls.console;

import com.devonfw.ide.gui.helper.FxHelper;
import com.devonfw.tools.ide.log.IdeLogEntry;

import com.devonfw.tools.ide.log.IdeLogLevel;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View-model for the console control.
 *
 * <p>Collects log entries produced by the application and exposes them as an observable, read-only list
 * suitable for binding to UI controls. Messages are buffered to avoid flooding the JavaFX application thread and flushed in batches via FxHelper.</p>
 */
public class ConsoleViewModel {

  private final ObservableList<IdeLogEntry> logEntries = FXCollections.observableArrayList();
  private final ObservableList<IdeLogEntry> readOnlyLogEntries = FXCollections.unmodifiableObservableList(logEntries);
  private final SimpleBooleanProperty autoScrollEnabled = new SimpleBooleanProperty(true);
  private final SimpleIntegerProperty lineCount = new SimpleIntegerProperty(logEntries.size());

  /** Whether a batch flush is already scheduled on the FX thread. */
  private boolean flushPending;

  /**
   * Thread-safe buffer collecting messages that arrive while a batched UI update is already pending. Keeps the FX thread from being overwhelmed by individual
   * {@link javafx.application.Platform#runLater(Runnable)} submissions.
   */
  private final Deque<IdeLogEntry> outputBuffer = new ArrayDeque<>();


  public ConsoleViewModel() {
    logEntries.addListener((ListChangeListener<IdeLogEntry>) change -> {
      while (change.next()) {
        if (change.wasAdded() || change.wasRemoved()) {
          lineCount.set(logEntries.size());
        }
      }
    });
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
   * Prints a simple message to the console.
   *
   * @param message message to be printed
   */
  public void appendOutput(String message) {

    appendOutput(new IdeLogEntry(IdeLogLevel.INFO, message));
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
  }

  /**
   * Gets an unmodifiable observable list of log entries currently shown in the console.
   *
   * @return read-only observable list of IdeLogEntry instances suitable for UI binding
   */
  public ObservableList<IdeLogEntry> logEntries() {
    return readOnlyLogEntries;
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
   * Property that controls whether the console should automatically scroll to the end when new entries are appended. Bind this to the UI toggle controlling
   * auto-scroll behaviour.
   *
   * @return the auto-scroll enabled boolean property
   */
  public SimpleBooleanProperty autoScrollEnabledProperty() {
    return autoScrollEnabled;
  }

  /**
   * Property exposing the current number of displayed log entries. Updated automatically when entries are added or removed; suitable for binding to UI labels
   * or status indicators.
   *
   * @return integer property containing the current line count
   */
  public SimpleIntegerProperty lineCountProperty() {
    return lineCount;
  }
}
