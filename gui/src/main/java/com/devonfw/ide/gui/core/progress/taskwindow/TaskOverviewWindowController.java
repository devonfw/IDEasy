<<<<<<<< HEAD:gui/src/main/java/com/devonfw/ide/gui/ui/progress/taskwindow/TaskOverviewWindowController.java
package com.devonfw.ide.gui.ui.progress.taskwindow;
========
package com.devonfw.ide.gui.core.progress.taskwindow;
>>>>>>>> main:gui/src/main/java/com/devonfw/ide/gui/core/progress/taskwindow/TaskOverviewWindowController.java

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

<<<<<<<< HEAD:gui/src/main/java/com/devonfw/ide/gui/ui/progress/taskwindow/TaskOverviewWindowController.java
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.ui.progress.ProgressBarTask;
========
import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.progress.ProgressBarTask;
>>>>>>>> main:gui/src/main/java/com/devonfw/ide/gui/core/progress/taskwindow/TaskOverviewWindowController.java

/**
 * Controller for the task overview window, which shows all currently running tasks and their progressbar.
 */
public class TaskOverviewWindowController {

  @FXML
  private ListView<ProgressBarTask> taskList;
  private final TaskManager taskManager;

  /**
   * @param taskManager the {@link TaskManager} to link to this TaskOverviewWindow.
   */
  public TaskOverviewWindowController(TaskManager taskManager) {

    this.taskManager = taskManager;
  }

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
