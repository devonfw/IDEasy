package com.devonfw.ide.gui.core.progress.taskwindow;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import com.devonfw.ide.gui.core.progress.TaskListViewModel;
import com.devonfw.ide.gui.core.progress.TaskViewModel;

/**
 * Controller for the task overview window, which shows all currently running tasks and their progressbar.
 */
public class TaskOverviewWindowController {

  @FXML
  private ListView<TaskViewModel> taskList;

  private final TaskListViewModel taskListViewModel;

  /**
   * @param taskListViewModel the {@link TaskListViewModel} providing the tasks to display.
   */
  public TaskOverviewWindowController(TaskListViewModel taskListViewModel) {

    this.taskListViewModel = Objects.requireNonNull(taskListViewModel);
  }

  @FXML
  private void initialize() {

    // the items are updated in place by the view model and the cells bind to their properties, so no manual refresh is required
    taskList.setCellFactory(new TaskWindowCellFactory());
    taskList.setItems(this.taskListViewModel.getTasks());
  }
}
