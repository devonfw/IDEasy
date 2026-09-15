package com.devonfw.ide.gui.ui.controls.console;

import javafx.collections.ListChangeListener.Change;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * Controller that manages an instance of a console using ListView for better performance with large outputs.
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
    lineCountLabel.textProperty().bind(viewModel.lineCountProperty().asString().concat(" lines"));
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

    if(viewModel.autoScrollEnabledProperty().get()) {
      if (change.next() && change.wasAdded()) {
        consoleListView.scrollTo(change.getTo() - 1);
      }
    }
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
