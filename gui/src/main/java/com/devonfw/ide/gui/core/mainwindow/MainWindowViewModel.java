package com.devonfw.ide.gui.core.mainwindow;

import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.factory.TabFactory;
import com.devonfw.ide.gui.core.progress.TaskListViewModel;
import com.devonfw.ide.gui.core.progress.TaskViewModel;
import com.devonfw.ide.gui.core.service.NlsService;

/**
 * View model of the main window. It holds the observable state behind the status bar (status text, progress and clickability) and the console visibility, and
 * reflects the task list held by the {@link GuiStateManager} onto the status bar.
 */
public class MainWindowViewModel {

  /** The running tasks. Reports additions, removals and progress changes of its items (see {@link TaskListViewModel}). */
  private final ObservableList<TaskViewModel> tasks;

  private final NlsService nlsService;

  private final TabFactory tabFactory;

  private final StringProperty statusText = new SimpleStringProperty();

  private final BooleanProperty statusProgressVisible = new SimpleBooleanProperty(false);

  private final DoubleProperty statusProgress = new SimpleDoubleProperty(0);

  private final BooleanProperty statusClickable = new SimpleBooleanProperty(false);

  private final BooleanProperty consoleVisible = new SimpleBooleanProperty(false);

  /**
   * Initializes the view model and wires the task-list listener that drives the status bar.
   *
   * @param guiStateManager the app-wide selection and context holder.
   */
  public MainWindowViewModel(GuiStateManager guiStateManager) {

    this.nlsService = Objects.requireNonNull(guiStateManager.getNlsService());
    this.tabFactory = Objects.requireNonNull(guiStateManager.getTabFactory());
    this.tabFactory.setPreLaunchAction(() -> this.setConsoleVisible(true));

    this.tasks = Objects.requireNonNull(guiStateManager.getTaskListViewModel()).getTasks();
    this.tasks.addListener((InvalidationListener) _ -> updateStatus());
    updateStatus();
  }

  /**
   * Reflects the tasks of the {@link TaskListViewModel} onto the status bar. Runs on the JavaFX thread.
   */
  private void updateStatus() {

    if (this.tasks.size() > 1) {
      this.statusText.set(this.tasks.size() + " " + this.nlsService.get("tasks_running"));
      this.statusClickable.set(true);
      this.statusProgressVisible.set(false);
    } else if (this.tasks.size() == 1) {
      TaskViewModel task = this.tasks.getFirst();
      this.statusText.set(task.getDescription());
      this.statusClickable.set(false);
      this.statusProgressVisible.set(true);
      this.statusProgress.set(task.getProgress());
    } else {
      this.statusText.set(this.nlsService.get("ideasy_ready_msg"));
      this.statusClickable.set(false);
      this.statusProgressVisible.set(false);
    }
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
