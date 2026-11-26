package com.devonfw.ide.gui.core.mainwindow.console;


import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javafx.collections.ListChangeListener.Change;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

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


    private static final List<String> LOG_LEVEL_STYLE_CLASSES =
        Arrays.stream(IdeLogLevel.values()).map(LogEntryCell::toStyleClass).toList();
    private final LogEntryTextArea textArea = new LogEntryTextArea();

    private LogEntryCell() {
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }


    @Override
    protected void updateItem(IdeLogEntry entry, boolean empty) {
      this.textArea.getStyleClass().removeAll(LOG_LEVEL_STYLE_CLASSES);
      super.updateItem(entry, empty);
      if (empty || entry == null) {
        this.textArea.clear();
        setGraphic(null);
      } else {
        this.textArea.setText(entry.displayValue());
        if (entry.level() != null) {
          this.textArea.getStyleClass().add(toStyleClass(entry.level()));
        }
        setGraphic(this.textArea);
      }
    }

    private static String toStyleClass(IdeLogLevel level) {

      return level.name().toLowerCase(Locale.ROOT);
    }
  }

  private static class LogEntryTextArea extends TextArea {
    private final Text textMeasure = new Text();
    private LogEntryTextArea() {
      setWrapText(true);
      setEditable(false);
      setFocusTraversable(false);
      setMinHeight(USE_PREF_SIZE);
      getStyleClass().add("log-entry");
    }
    @Override
    public Orientation getContentBias() {

      return Orientation.HORIZONTAL;
    }
    @Override
    protected double computePrefHeight(double width){
      Region scrollPane = (Region) lookup(".scroll-pane");
      Region content = (Region) lookup(".content");
      Text text = (Text) lookup(".text");
      if ((scrollPane == null) || (content == null) || (text == null)) {
        return super.computePrefHeight(width);
      }
      double wrappingWidth = 0; // without a given width the text is measured unwrapped
      if (width >= 0) {
        double horizontalInsets = snappedLeftInset() + snappedRightInset() + getHorizontalInsets(scrollPane) + getHorizontalInsets(content);
        wrappingWidth = Math.max(width - horizontalInsets, 0);
      }
      this.textMeasure.setFont(text.getFont());
      this.textMeasure.setBoundsType(text.getBoundsType());
      this.textMeasure.setLineSpacing(text.getLineSpacing());
      this.textMeasure.setText(getText());
      this.textMeasure.setWrappingWidth(wrappingWidth);
      double verticalInsets = snappedTopInset() + snappedBottomInset() + getVerticalInsets(scrollPane) + getVerticalInsets(content);
      return snapSizeY(this.textMeasure.getLayoutBounds().getHeight() + verticalInsets);
    }
    private static double getHorizontalInsets(Region region) {

      Insets insets = region.getInsets();
      return region.snapSpaceX(insets.getLeft()) + region.snapSpaceX(insets.getRight());
    }

    private static double getVerticalInsets(Region region) {

      Insets insets = region.getInsets();
      return region.snapSpaceY(insets.getTop()) + region.snapSpaceY(insets.getBottom());
    }
  }
}
