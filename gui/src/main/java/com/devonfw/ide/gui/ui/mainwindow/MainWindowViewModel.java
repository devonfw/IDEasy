package com.devonfw.ide.gui.ui.mainwindow;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.helper.combobox.ComboBoxViewModel;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.modal.IdeDialog;
import com.devonfw.ide.gui.ui.progress.ProgressBarTask;

/**
 * View model of the main window. It holds the observable state behind the navigation panel (project, workspace and language selection) and the status bar,
 * and triggers a context switch in the {@link GuiStateManager} whenever a project/workspace pair is selected.
 */
public class MainWindowViewModel {

  private static final Logger LOG = LoggerFactory.getLogger(MainWindowViewModel.class);

  private static final String DEFAULT_WORKSPACE = "main";

  private final GuiStateManager guiStateManager;

  private final ProjectManager projectManager;

  private final NlsService nlsService;

  private final ComboBoxViewModel<String> projects = new ComboBoxViewModel<>();

  private final ComboBoxViewModel<String> workspaces = new ComboBoxViewModel<>();

  private final ComboBoxViewModel<String> languages = new ComboBoxViewModel<>();

  private final Map<String, Locale> languageMap = new LinkedHashMap<>();

  private final StringProperty statusText = new SimpleStringProperty();

  private final BooleanProperty statusProgressVisible = new SimpleBooleanProperty(false);

  private final DoubleProperty statusProgress = new SimpleDoubleProperty(0);

  private final BooleanProperty statusClickable = new SimpleBooleanProperty(false);

  private final BooleanProperty consoleVisible = new SimpleBooleanProperty(false);


  /**
   * Initializes the view model, populates the project, workspace and language combo boxes, and wires the selection listeners that drive context switching.
   *
   * @param guiStateManager the app-wide selection and context holder.
   * @param projectManager resolves available project and workspace names.
   * @param nlsService the localization service.
   */
  public MainWindowViewModel(GuiStateManager guiStateManager, ProjectManager projectManager, NlsService nlsService) {

    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.projectManager = Objects.requireNonNull(projectManager);
    this.nlsService = Objects.requireNonNull(nlsService);

    this.statusText.set(this.nlsService.get("ideasy_ready_msg"));

    initProjectsComboBox();
    initLanguageComboBox();
    setUpTaskListListener();

    // Registered after the combo boxes are initialized so that the initial bindBidirectional does not already trigger a context switch.
    this.projects.selectedItemProperty().addListener((_, _, project) -> {
      if (project != null) {
        populateWorkspaceComboBox();
      }
    });
    this.workspaces.selectedItemProperty().addListener((_, _, workspace) -> {
      this.guiStateManager.workspaceSelectedProperty().set(workspace != null);
      if (workspace != null) {
        updateContext();
      }
    });
  }

  private void initProjectsComboBox() {

    this.projects.getItems().clear();
    this.projects.getItems().addAll(this.projectManager.getProjectNames());

    this.workspaces.isDisabledProperty().bind(this.projects.selectedItemProperty().isNull());
  }

  private void populateWorkspaceComboBox() {

    List<String> loadedWorkspaces = loadWorkspaces(this.projects.getSelectedItem().orElse(null));

    this.workspaces.clearSelection();
    this.workspaces.getItems().clear();
    this.workspaces.getItems().addAll(loadedWorkspaces);

    // Default to the "main" workspace when it exists.
    if (loadedWorkspaces.contains(DEFAULT_WORKSPACE)) {
      this.workspaces.select(DEFAULT_WORKSPACE);
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
    this.languages.select(resolveLanguageSelection(this.nlsService.getLocale()));
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

  private void updateContext() {

    String project = this.projects.getSelectedItem().orElse(null);
    String workspace = this.workspaces.getSelectedItem().orElse(null);

    if (project == null || workspace == null) {
      this.guiStateManager.workspaceSelectedProperty().set(false);
      return;
    }

    try {
      this.guiStateManager.switchContext(project, workspace);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }

  private void setUpTaskListListener() {

    ListChangeListener<ProgressBarTask> taskListChangeListener = change -> {
      List<ProgressBarTask> tasks = this.guiStateManager.getTaskManager().getTasks();

      while (change.next()) {
        if (change.wasAdded()) {

          for (ProgressBarTask progressTask : change.getAddedSubList()) {
            progressTask.currentProgressProperty().addListener((_, _, _) ->
                updateStatus(tasks)
            );
          }
          updateStatus(tasks);
        } else if (change.wasRemoved()) {

          updateStatus(tasks);
        } else if (change.wasUpdated()) {

          updateStatus(tasks);
        }
      }
    };
    this.guiStateManager.getTaskManager().getTasks().addListener(taskListChangeListener);
  }

  private void updateStatus(List<ProgressBarTask> taskList) {

    Platform.runLater(() -> {

      if (taskList.size() > 1) {
        this.statusText.set(taskList.size() + " tasks running...");
        this.statusClickable.set(true);
        this.statusProgressVisible.set(false);
      } else if (taskList.size() == 1) {
        ProgressBarTask task = taskList.getFirst();
        this.statusText.set(String.format(
            ProgressBarTask.TASK_DESCRIPTION_STRING_FORMAT,
            task.getTitle(),
            task.getCurrentProgress(),
            task.getMaxSize(),
            task.getUnitName())
        );
        this.statusClickable.set(false);
        this.statusProgressVisible.set(true);
        this.statusProgress.set((double) (task.getCurrentProgress()) / task.getMaxSize());
      } else {
        this.statusText.set(this.nlsService.get("ideasy_ready_msg"));
        this.statusClickable.set(false);
        this.statusProgressVisible.set(false);
      }
    });
  }

  /**
   * @return the view model backing the project {@link ComboBox}.
   */
  public ComboBoxViewModel<String> getProjectsViewModel() {

    return this.projects;
  }

  /**
   * @return the view model backing the workspace {@link ComboBox}.
   */
  public ComboBoxViewModel<String> getWorkspacesViewModel() {

    return this.workspaces;
  }

  /**
   * @return the view model backing the language {@link ComboBox}.
   */
  public ComboBoxViewModel<String> getLanguagesViewModel() {

    return this.languages;
  }

  /**
   * @return the property holding the status bar text.
   */
  public StringProperty statusTextProperty() {

    return this.statusText;
  }

  /**
   * @return the property flagging whether the status bar progress indicator is visible.
   */
  public BooleanProperty statusProgressVisibleProperty() {

    return this.statusProgressVisible;
  }

  /**
   * @return the property holding the progress of the currently running task (0 to 1).
   */
  public DoubleProperty statusProgressProperty() {

    return this.statusProgress;
  }

  /**
   * @return the property flagging whether the status bar is clickable to open the task overview.
   */
  public BooleanProperty statusClickableProperty() {

    return this.statusClickable;
  }

  /**
   * @return the property flagging whether the console pane is visible.
   */
  public BooleanProperty consoleVisibleProperty() {

    return this.consoleVisible;
  }

  /**
   * Sets whether the console pane is visible.
   *
   * @param visible the new visibility of the console pane.
   */
  public void setConsoleVisible(boolean visible) {

    this.consoleVisible.set(visible);
  }

}
