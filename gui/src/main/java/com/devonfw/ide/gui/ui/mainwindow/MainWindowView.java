package com.devonfw.ide.gui.ui.mainwindow;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.factory.TabFactory;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;
import com.devonfw.ide.gui.ui.controls.mainwindow.NavigationPanelControl;
import com.devonfw.ide.gui.ui.progress.taskwindow.TaskOverviewWindow;

/**
 * View of the main window. It renders what the {@link MainWindowViewModel} exposes and translates user gestures back onto it; all selection state, status
 * computation and context switching live in the view model. The split-pane geometry backing the console stays here, because divider positions are a purely
 * visual concern.
 */
public class MainWindowView extends BorderPane {

  private static final Logger LOG = LoggerFactory.getLogger(MainWindowView.class);

  /** Divider position that reveals the console panel. */
  private static final double CONSOLE_SHOWN_DIVIDER_POSITION = 0.75;

  /** Divider position that collapses the console panel. */
  private static final double CONSOLE_HIDDEN_DIVIDER_POSITION = 1.0;

  /**
   * Threshold below which the console counts as visible. Dragging the divider fully down does not yield exactly 1.0 in JavaFX, but something like 0.9935, so an
   * exact comparison would never report the console as hidden.
   */
  private static final double CONSOLE_HIDDEN_THRESHOLD = 0.99;

  /** Only expand the console when the divider is (nearly) collapsed, so an already open console keeps the size the user chose. */
  private static final double CONSOLE_COLLAPSED_THRESHOLD = 0.9;

  private static final double PROGRESSBAR_VISIBLE_WIDTH = 150.0;

  @FXML
  private NavigationPanelControl navigationPanelControl;

  @FXML
  private TabPane tabContainer;

  @FXML
  private SplitPane centerSplitPane;

  @FXML
  private ToggleButton consolePaneToggleButton;

  @FXML
  private AnchorPane console;

  @FXML
  private Label statusLabel;

  @FXML
  private ProgressBar statusProgressBar;

  private final MainWindowViewModel viewModel;

  private final NlsService nlsService;

  private final ConsoleController consoleController;

  private final TabFactory tabFactory;

  private final Divider centerDivider;

  private final GuiStateManager guiStateManager;

  private boolean propagatingDividerPosition;

  public MainWindowView(MainWindowViewModel viewModel, GuiStateManager guiStateManager, NlsService nlsService,
      ConsoleController consoleController, TabFactory tabFactory) {

    super();

    this.viewModel = Objects.requireNonNull(viewModel);
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.nlsService = Objects.requireNonNull(nlsService);
    this.consoleController = Objects.requireNonNull(consoleController);
    this.tabFactory = Objects.requireNonNull(tabFactory);

    final FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindowView.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    loader.setResources(this.nlsService.getResourceBundle());
    loader.setControllerFactory(clazz -> clazz == ConsoleController.class ? this.consoleController : null);

    try {
      loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.tabFactory.attach(this.tabContainer);
    this.tabFactory.openLauncherTab();
    // Show the console before launching an IDE (restores the previous behavior).
    this.tabFactory.setPreLaunchAction(() -> this.viewModel.setConsoleVisible(true));

    this.centerDivider = this.centerSplitPane.getDividers().getFirst();

    bindConsole();
    bindStatusBar();
    this.navigationPanelControl.bind(this.viewModel);
  }

  private void bindConsole() {

    this.centerDivider.positionProperty().addListener((_, _, position) -> {
      this.propagatingDividerPosition = true;
      try {
        // The console pane is never hidden today, but the original visibility check is kept so dragging cannot report a hidden pane as shown.
        this.viewModel.setConsoleVisible(position.doubleValue() < CONSOLE_HIDDEN_THRESHOLD && this.console.isVisible());
      } finally {
        this.propagatingDividerPosition = false;
      }
    });
    this.viewModel.consoleVisibleProperty().addListener((_, _, visible) -> {
      if (!this.propagatingDividerPosition) {
        applyConsoleVisibility(visible);
      }
    });
    this.consolePaneToggleButton.selectedProperty().bindBidirectional(this.viewModel.consoleVisibleProperty());
  }

  private void applyConsoleVisibility(boolean visible) {

    if (visible) {
      if (this.centerDivider.getPosition() >= CONSOLE_COLLAPSED_THRESHOLD) {
        this.centerSplitPane.setDividerPosition(0, CONSOLE_SHOWN_DIVIDER_POSITION);
      }
      LOG.debug("Console shown");
    } else {
      this.centerSplitPane.setDividerPosition(0, CONSOLE_HIDDEN_DIVIDER_POSITION);
      LOG.debug("Console hidden");
    }
  }

  private void bindStatusBar() {

    this.statusLabel.textProperty().bind(this.viewModel.statusTextProperty());
    this.statusLabel.underlineProperty().bind(this.viewModel.statusClickableProperty());
    this.statusProgressBar.visibleProperty().bind(this.viewModel.statusProgressVisibleProperty());
    this.statusProgressBar.progressProperty().bind(this.viewModel.statusProgressProperty());

    applyStatusLabelStyle(this.viewModel.statusClickableProperty().get());
    this.viewModel.statusClickableProperty().addListener((_, _, clickable) -> applyStatusLabelStyle(clickable));

    this.statusProgressBar.prefWidthProperty().bind(
        Bindings.when(this.viewModel.statusProgressVisibleProperty()).then(PROGRESSBAR_VISIBLE_WIDTH).otherwise(0.0));

    this.statusLabel.setOnMouseClicked(_ -> {
      if (this.viewModel.statusClickableProperty().get()) {
        TaskOverviewWindow.getInstance(this.guiStateManager.getTaskManager()).showRelativeToReferenceNode(this.statusLabel);
      }
    });
  }

  private void applyStatusLabelStyle(boolean clickable) {

    if (clickable) {
      this.statusLabel.setStyle(
          "-fx-text-fill: blue;"
              + "-fx-cursor: hand"
      );
    } else {
      this.statusLabel.setStyle("");
    }
  }

}
