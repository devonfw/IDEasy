package com.devonfw.ide.gui.ui.mainwindow;

import java.util.List;
import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.factory.TabFactory;
import com.devonfw.ide.gui.helper.combobox.ComboBoxViewModel;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.progress.ProgressBarTask;

/**
 * View model of the main window. It holds the observable state behind the status bar (status text, progress and clickability) and the console visibility, and
 * reflects the task list held by the {@link GuiStateManager} onto the status bar.
 */
public class MainWindowViewModel {

  private final GuiStateManager guiStateManager;

  private final NlsService nlsService;

  private final TabFactory tabFactory;

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
   * Initializes the view model and wires the task-list listener that drives the status bar.
   *
   * @param guiStateManager the app-wide selection and context holder.
   * @param nlsService the localization service.
   */
  public MainWindowViewModel(GuiStateManager guiStateManager, NlsService nlsService, TabFactory tabFactory) {

    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.nlsService = Objects.requireNonNull(nlsService);
    this.tabFactory = Objects.requireNonNull(tabFactory);

    this.statusText.set(this.nlsService.get("ideasy_ready_msg"));

    setUpTaskListListener();
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

  public void openLauncherTab() {

    this.tabFactory.openLauncherTab();
  }

}
