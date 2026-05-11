package com.devonfw.ide.gui.progress.taskwindow;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.TaskManager;

/**
 * Controller for the task overview window, which shows all currently running tasks and their progressbars.
 */
public class TaskOverviewWindowController {

  @FXML
  public ListView<ProgressBarTask> taskList;
  private final TaskManager taskManager = TaskManager.getInstance();

  @FXML
  private void initialize() {

    taskList.setCellFactory(new TaskWindowCellFactory());

    /* This part...
       1. connects the task list to the UI, automatically reacting to additions and removals
       2. also sets an Observable on progress property, so the UI also gets updated in case it changes
     */
    ObservableList<ProgressBarTask> tasks = taskManager.getTasks();
    FXCollections.observableList(
        tasks,
        task -> new Observable[] {
            task.currentProgressProperty()
        }
    );

    taskList.setItems(tasks);
  }
}
