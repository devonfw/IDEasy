package com.devonfw.ide.gui.core.mainwindow.console;


import java.text.MessageFormat;

import javafx.collections.ListChangeListener.Change;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

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


    private final TextArea textArea;
    public LogEntryCell(){
      textArea = new TextArea();
      textArea.setWrapText(true);
      textArea.setEditable(false);
      textArea.setFocusTraversable(false);
    }

    private static double computeTextHeight(String text, double width) {

      Text helper = new Text(text);

      helper.setFont(Font.font("Consolas", 11));
      helper.setWrappingWidth(width);

      return helper.getLayoutBounds().getHeight();
    }
    @Override
    protected void updateItem(IdeLogEntry entry, boolean empty) {

      super.updateItem(entry, empty);
      if (empty || entry == null) {
        textArea.clear();
        setGraphic(null);
      } else {
        String text= entry.displayValue();
        textArea.setText(text);
        textArea.getStyleClass().removeIf(style ->
            style.startsWith("log-entry"));

        textArea.getStyleClass().add("log-entry");
        textArea.getStyleClass().add(getStyleClassForEntry(entry));
        textArea.setText(text);

        textArea.applyCss();

        double textHeight = computeTextHeight(text, textArea.getWidth());

        textArea.setPrefHeight(textHeight + 11);


        setGraphic(textArea);
      }
    }

    private String getStyleClassForEntry(IdeLogEntry entry) {

      if (entry.level() == null) {
        return "log-entry-plain";
      }
      return switch (entry.level()) {
        case ERROR -> "log-entry-error";
        case WARNING -> "log-entry-warning";
        case INFO -> "log-entry-info";
        case DEBUG -> "log-entry-debug";
        case TRACE -> "log-entry-trace";
        default -> "log-entry-plain";
      };
    }
  }
}
