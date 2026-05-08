package com.devonfw.ide.gui.progress.taskwindow;

import java.util.Objects;
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
  private TaskManager taskManager = TaskManager.getInstance();

  public TaskOverviewWindowController() {

    taskManager.addListener(this);
  }

  @FXML
  private void initialize() {

    taskList.setCellFactory(new TaskWindowCellFactory());
    taskList.getItems().addAll(TaskManager.getInstance().getTasks());
  }

  @Override
  public void onProgressTaskUpdated(GuiProgressBarHandling updatedTask, long stepPosition) {

    taskList.getItems().stream()
        .filter(task -> Objects.equals(task.getTaskId(), updatedTask.getTaskId()))
        .findFirst()
        .ifPresent(task -> {
          int index = taskList.getItems().indexOf(task);
          taskList.getItems().set(index, updatedTask);
        });
  }

  @Override
  public void onProgressTaskAdded(GuiProgressBarHandling task) {

    taskList.getItems().setAll(taskManager.getTasks());
  }

  @Override
  public void onProgressTaskRemoved(GuiProgressBarHandling task) {

    taskList.getItems().setAll(taskManager.getTasks());
  }
}
