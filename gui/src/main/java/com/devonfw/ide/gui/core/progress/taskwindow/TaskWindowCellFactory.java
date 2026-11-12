package com.devonfw.ide.gui.core.progress.taskwindow;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import com.devonfw.ide.gui.core.progress.TaskViewModel;

/**
 * Cell factory for displaying a list of tasks in the {@link TaskOverviewWindow}. A cell binds to the properties of its {@link TaskViewModel}, so it follows
 * the progress of its task.
 */
public class TaskWindowCellFactory implements Callback<ListView<TaskViewModel>, ListCell<TaskViewModel>> {

  @Override
  public ListCell<TaskViewModel> call(ListView<TaskViewModel> param) {
    return new ListCell<>() {

      final ProgressBar progressBar = new ProgressBar();
      final Label titleLabel = new Label();
      final VBox contentBox = new VBox(5, titleLabel, progressBar);

      final HBox root = new HBox(10, contentBox);

      {
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        root.setAlignment(Pos.CENTER_LEFT);
        //set the size of the progress bar to fill the window completely
        progressBar.setMaxWidth(Double.MAX_VALUE);
      }

      @Override
      public void updateItem(TaskViewModel task, boolean empty) {
        super.updateItem(task, empty);

        titleLabel.textProperty().unbind();
        progressBar.progressProperty().unbind();
        if (empty || task == null) {
          setText(null);
          setGraphic(null);
        } else {
          titleLabel.textProperty().bind(task.descriptionProperty());
          progressBar.progressProperty().bind(task.progressProperty());
          setGraphic(root);
        }
      }
    };
  }
}
