package com.devonfw.ide.gui.core.mainwindow.console;

import java.text.MessageFormat;
import java.util.stream.Collectors;

import javafx.collections.ListChangeListener.Change;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * View of the console
 */
public class ConsoleView {

  private static final KeyCombination COPY_SHORTCUT = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

  private final NlsService nlsService;
  private final ConsoleViewModel viewModel;

  /** Index of the log entry where the current mouse drag started or {@code -1} if no selection by dragging is in progress. */
  private int dragAnchorIndex = -1;

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
    consoleListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    consoleListView.addEventHandler(KeyEvent.KEY_PRESSED, this::handleCopy);
    consoleListView.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
    consoleListView.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
    viewModel.logEntries().addListener(this::handleScroll);
  }

  /**
   * Remembers the entry where a plain mouse press started so that dragging can extend the selection from there.
   */
  private void handleMousePressed(MouseEvent event) {

    if (event.isPrimaryButtonDown() && !event.isShiftDown() && !event.isShortcutDown()) {
      this.dragAnchorIndex = getIndexAt(event);
    } else {
      this.dragAnchorIndex = -1;
    }
  }

  /**
   * Selects all entries between the one where the drag started and the one currently below the mouse.
   */
  private void handleMouseDragged(MouseEvent event) {

    if (this.dragAnchorIndex < 0) {
      return;
    }
    int index = getIndexAt(event);
    if (index >= 0) {
      MultipleSelectionModel<IdeLogEntry> selectionModel = consoleListView.getSelectionModel();
      selectionModel.clearSelection();
      selectionModel.selectRange(Math.min(this.dragAnchorIndex, index), Math.max(this.dragAnchorIndex, index) + 1);
    }
  }

  /**
   * @return the index of the log entry below the mouse or {@code -1} if there is none.
   */
  private int getIndexAt(MouseEvent event) {

    Node node = event.getPickResult().getIntersectedNode();
    while ((node != null) && (node != consoleListView)) {
      if (node instanceof ListCell<?> cell) {
        return cell.isEmpty() ? -1 : cell.getIndex();
      }
      node = node.getParent();
    }
    return -1;
  }

  /**
   * Copies the selected log entries (one per line) to the clipboard when the copy shortcut is pressed.
   */
  private void handleCopy(KeyEvent event) {

    if (COPY_SHORTCUT.match(event)) {
      String selectedText = consoleListView.getSelectionModel().getSelectedItems().stream().map(IdeLogEntry::displayValue)
          .collect(Collectors.joining(System.lineSeparator()));
      if (!selectedText.isEmpty()) {
        ClipboardContent content = new ClipboardContent();
        content.putString(selectedText);
        Clipboard.getSystemClipboard().setContent(content);
      }
      event.consume();
    }
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
