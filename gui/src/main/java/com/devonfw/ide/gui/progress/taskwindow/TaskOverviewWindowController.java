package com.devonfw.ide.gui.progress.taskwindow;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import com.devonfw.ide.gui.progress.GuiProgressBarHandling;
import com.devonfw.ide.gui.progress.TaskManager;
import com.devonfw.ide.gui.progress.TaskManager.ProgressListener;

/**
 * Controller for the task overview window, which shows all currently running tasks and their progressbars.
 */
public class TaskOverviewWindowController implements ProgressListener {

  @FXML
  public ListView<GuiProgressBarHandling> taskList;

  public TaskOverviewWindowController() {

    TaskManager.getInstance().addListener(this);
  }

  @FXML
  private void initialize() {

    taskList.setCellFactory(new TaskWindowCellFactory());
    taskList.getItems().addAll(TaskManager.getInstance().getTasks());
  }

  @Override
  public void onProgressTaskUpdated(GuiProgressBarHandling updatedTask, long stepPosition) {

    // No need to update the list, as the progress bars are bound to the task properties and will update automatically.
  }

  @Override
  public void onProgressTaskAdded(List<GuiProgressBarHandling> updatedTaskList) {

    taskList.getItems().setAll(TaskManager.getInstance().getTasks());
  }

  @Override
  public void onProgressTaskRemoved(List<GuiProgressBarHandling> updatedTaskList) {

    taskList.getItems().setAll(TaskManager.getInstance().getTasks());
  }
}
