package com.devonfw.ide.gui.ui.mainwindow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert.AlertType;
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
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.factory.TabFactory;
import com.devonfw.ide.gui.helper.combobox.ComboBoxViewModel;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;
import com.devonfw.ide.gui.ui.controls.mainwindow.NavigationPanelControl;
import com.devonfw.ide.gui.ui.modal.IdeDialog;
import com.devonfw.ide.gui.ui.progress.ProgressBarTask;
import com.devonfw.ide.gui.ui.progress.taskwindow.TaskOverviewWindow;


public class MainWindow extends BorderPane {

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

  private ComboBoxViewModel<String> projects = new ComboBoxViewModel<>();
  private ComboBoxViewModel<String> workspaces = new ComboBoxViewModel<>();
  private ComboBoxViewModel<String> languages = new ComboBoxViewModel<>();

  private final GuiStateManager guiStateManager;
  private final NlsService nlsService;
  private final ProjectManager projectManager;

  private final Map<String, Locale> languageMap;

  private final double PROGRESSBAR_VISIBLE_WIDTH = 150.0;

  private final Divider centerDivider;

  private static final Logger LOG = LoggerFactory.getLogger(MainWindow.class);

  private final ConsoleController consoleController;
  private final TabFactory tabFactory;

  public MainWindow(GuiStateManager guiStateManager, ProjectManager projectManager, NlsService nlsService) {
    super();

    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.projectManager = projectManager;
    this.nlsService = Objects.requireNonNull(nlsService);
    this.consoleController = new ConsoleController(this.nlsService);
    this.tabFactory = new TabFactory(this.guiStateManager, this.nlsService, this.consoleController);
    this.languageMap = new LinkedHashMap<>();

    final FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
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

    this.centerDivider = this.centerSplitPane.getDividers().getFirst();
    this.centerDivider.positionProperty().addListener((_, _, newVal) -> {
      //This is a bit of a weird behaviour in JavaFX, but even if you drag the divider fully down,
      // the position value does not become 1, but something like 0.9935345
      this.consolePaneToggleButton.setSelected(newVal.doubleValue() < 0.99);
    });

    this.consolePaneToggleButton.setOnAction(_ -> toggleConsole());
    // Show the console before launching an IDE (restores the previous behavior).
    this.tabFactory.setPreLaunchAction(this::showConsole);

    setUpTaskListListener();
    initProjectsComboBox();
    initLanguageComboBox();
    navigationPanelControl.bind(this);
  }

  private void initProjectsComboBox() {
    this.projects.getItems().clear();
    this.projects.getItems().addAll(this.projectManager.getProjectNames());
    this.projects.selectedItemProperty().bindBidirectional(this.guiStateManager.selectedProjectProperty());

    this.workspaces.selectedItemProperty().bindBidirectional(this.guiStateManager.selectedWorkspaceProperty());
    this.workspaces.isDisabledProperty().bind(this.projects.selectedItemProperty().isNull());
  }

  public void populateWorkspaceComboBox() {

    List<String> workspaces = this.loadWorkspaces(projects.getSelectedItem().get());

    this.workspaces.clearSelection();
    this.workspaces.getItems().clear();
    this.workspaces.getItems().addAll(workspaces);

    // Default to the "main" workspace when it exists.
    if (workspaces.contains("main")) {
      this.workspaces.select("main");
    }

  }

  private List<String> loadWorkspaces(String project) {
    try {
      return this.projectManager.getWorkspaceNames(project);
    } catch (NotDirectoryException e) {
      throw new RuntimeException(e);
    }
  }

  private void initLanguageComboBox() {

    this.languageMap.clear();
    this.languages.getItems().clear();

    for (Locale locale : this.nlsService.getAvailableLocales()) {
      String displayName = this.nlsService.getLanguageDisplayName(locale);
      this.languageMap.put(displayName, locale);
    }

    this.languages.getItems().addAll(this.languageMap.keySet());
    //initial value
    this.languages.select(this.resolveLanguageSelection(this.nlsService.getLocale()));

  }

  private String resolveLanguageSelection(Locale currentLocale) {

    if (currentLocale == null) {
      return this.languageMap.keySet().stream().findFirst().orElse(null);
    }

    String languageMatch = null;

    for (Map.Entry<String, Locale> entry : this.languageMap.entrySet()) {
      Locale entryLocale = entry.getValue();
      // Exact language tag match takes priority
      if (entryLocale.toLanguageTag().equalsIgnoreCase(currentLocale.toLanguageTag())) {
        return entry.getKey();
      }
      // Track language-only match as fallback
      if (languageMatch == null) {
        if (entryLocale.getLanguage().equalsIgnoreCase(currentLocale.getLanguage())) {
          languageMatch = entry.getKey();
        }
      }
    }

    // Return language-only match if found, otherwise first available
    return languageMatch != null ? languageMatch : this.languageMap.keySet().stream().findFirst().orElse(null);
  }

  public void updateContext() {
    String project = this.projects.getSelectedItem().orElse(null);
    String workspace = this.workspaces.getSelectedItem().orElse(null);

    if (project == null || workspace == null) {
      this.guiStateManager.isWorkspaceSelectedProperty().set(false);
      return;
    }

    try {
      guiStateManager.switchContext(project, workspace);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }

  public ComboBoxViewModel<String> getProjectsViewModel() {
    return projects;
  }

  public ComboBoxViewModel<String> getWorkspacesViewModel() {
    return workspaces;
  }

  public ComboBoxViewModel<String> getLanguagesViewModel() {
    return languages;
  }

  private void setUpTaskListListener() {

    ListChangeListener<ProgressBarTask> taskListChangeListener = change -> {
      List<ProgressBarTask> tasks = this.guiStateManager.getTaskManager().getTasks();

      while (change.next()) {
        if (change.wasAdded()) {

          for (ProgressBarTask progressTask : change.getAddedSubList()) {
            progressTask.currentProgressProperty().addListener((_, _, _) ->
                updateStatusLabel(tasks)
            );
          }
          updateStatusLabel(tasks);
        } else if (change.wasRemoved()) {

          updateStatusLabel(tasks);
        } else if (change.wasUpdated()) {

          updateStatusLabel(tasks);
        }
      }
    };
    this.guiStateManager.getTaskManager().getTasks().addListener(taskListChangeListener);
  }

  private void updateStatusLabel(List<ProgressBarTask> taskList) {

    Platform.runLater(() -> {

      if (taskList.size() > 1) {
        this.statusLabel.setOnMouseClicked(
            e -> TaskOverviewWindow.getInstance(this.guiStateManager.getTaskManager()).showRelativeToReferenceNode(this.statusLabel));

        this.statusProgressBar.setVisible(false);
        this.statusProgressBar.setPrefWidth(0);
        this.statusLabel.setText(taskList.size() + " tasks running...");

        this.statusLabel.setUnderline(true);
        this.statusLabel.setStyle(
            "-fx-text-fill: blue;"
                + "-fx-cursor: hand"
        );
      } else if (taskList.size() == 1) {
        this.statusLabel.setOnMouseClicked(null);

        ProgressBarTask task = taskList.getFirst();
        this.statusLabel.setText(String.format(
            ProgressBarTask.TASK_DESCRIPTION_STRING_FORMAT,
            task.getTitle(),
            task.getCurrentProgress(),
            task.getMaxSize(),
            task.getUnitName())
        );
        this.statusLabel.setUnderline(false);
        this.statusLabel.setStyle("");

        this.statusProgressBar.setVisible(true);
        this.statusProgressBar.setPrefWidth(PROGRESSBAR_VISIBLE_WIDTH);
        this.statusProgressBar.setProgress((double) (task.getCurrentProgress()) / task.getMaxSize());
      } else {
        this.statusLabel.setOnMouseClicked(null);
        this.statusLabel.setText(this.nlsService.get("ideasy_ready_msg"));
        this.statusProgressBar.setVisible(false);
        this.statusProgressBar.setPrefWidth(0);

        this.statusLabel.setUnderline(false);
        this.statusLabel.setStyle("");
      }
    });
  }

  public void setIsWorkspaceSelected(boolean b) {
    this.guiStateManager.isWorkspaceSelectedProperty().setValue(b);
  }

  public void toggleConsole() {

    if (centerSplitPane != null) {
      if (isConsoleVisible()) {
        hideConsole();
      } else {
        showConsole();
      }
      consolePaneToggleButton.setSelected(isConsoleVisible());
    }
  }

  /**
   * Hides the console panel
   */
  public void hideConsole() {

    if (centerSplitPane != null) {
      centerSplitPane.setDividerPosition(0, 1.0);
      LOG.debug("Console hidden");
    }
  }

  /**
   * Shows the console panel
   */
  public void showConsole() {

    if (centerSplitPane != null) {
      if (centerSplitPane.getDividers().getFirst().getPosition() >= 0.9) {
        centerSplitPane.setDividerPosition(0, 0.75);
      }
      LOG.debug("Console shown");
    }
  }

  private boolean isConsoleVisible() {
    return centerDivider.getPosition() <= 0.99 && console.isVisible();
  }
}
