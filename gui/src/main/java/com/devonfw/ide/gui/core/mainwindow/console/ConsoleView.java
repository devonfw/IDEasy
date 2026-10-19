package com.devonfw.ide.gui.core.mainwindow.console;

import java.text.MessageFormat;

import javafx.collections.ListChangeListener.Change;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * View of the console
 */
public class ConsoleView {

  private final NlsService nlsService;
  private final ConsoleViewModel viewModel;

  @FXML
  private Button clearButton;

  @FXML
  private CheckBox autoScrollCheckBox;

  @FXML
  private Label lineCountLabel;

  @FXML
  private ListView<IdeLogEntry> consoleListView;


  /// @param viewModel console view model injection
  /// @param nlsService nlsService injection
  public ConsoleView(ConsoleViewModel viewModel, NlsService nlsService) {

    this.viewModel = viewModel;
    this.nlsService = nlsService;
  }

  @FXML
  private void initialize() {

    setupListView();
    setupEventHandlers();

    autoScrollCheckBox.selectedProperty().bindBidirectional(viewModel.autoScrollEnabledProperty());

    lineCountLabel.textProperty().bind(viewModel.lineCountProperty().asString().map(s -> MessageFormat.format(nlsService.get("console_line_count"), s)));
  }

  /**
   * Sets up the ListView with a custom cell factory for colored log levels.
   */
  private void setupListView() {

    consoleListView.setCellFactory(_ -> new LogEntryCell());
    consoleListView.setItems(viewModel.logEntries());
    viewModel.logEntries().addListener(this::handleScroll);
  }

  /**
   * Sets up event handlers for the buttons.
   */
  private void setupEventHandlers() {

    clearButton.setOnAction(_ -> viewModel.clearConsole());
  }

  /**
   * Scrolls to the end of the console.
   */
  private void handleScroll(Change<? extends IdeLogEntry> change) {

    if (viewModel.autoScrollEnabledProperty().get()) {
      if (change.next() && change.wasAdded()) {
        consoleListView.scrollTo(change.getTo() - 1);
      }
    }
  }

  /**
   * Custom ListCell for rendering log entries with colors based on log level.
   */
  private static class LogEntryCell extends ListCell<IdeLogEntry> {

    @Override
    protected void updateItem(IdeLogEntry entry, boolean empty) {

      super.updateItem(entry, empty);

      getStyleClass().removeAll(
          "base-style",
          "error-style",
          "warning-style",
          "info-style",
          "debug-style",
          "trace-style",
          "plain-style",
          "error-bg",
          "warning-bg");

      if (empty || entry == null) {
        setText(null);
        return;
      }

      setText(entry.displayValue());

      getStyleClass().add("base-style");

      switch (entry.level()) {
        case ERROR -> getStyleClass().addAll("error-style", "error-bg");
        case WARNING -> getStyleClass().addAll("warning-style", "warning-bg");
        case INFO -> getStyleClass().add("info-style");
        case DEBUG -> getStyleClass().add("debug-style");
        case TRACE -> getStyleClass().add("trace-style");
        default -> getStyleClass().add("plain-style");
      }
    }



  }
}
