package com.devonfw.ide.gui.progress.taskwindow;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.progress.ProgressBarTask;

/**
 * Cell factory for displaying a list of tasks in the {@link TaskOverviewWindow}
 */
public class TaskWindowCellFactory implements Callback<ListView<ProgressBarTask>, ListCell<ProgressBarTask>> {

  private static final Logger LOG = LoggerFactory.getLogger(TaskWindowCellFactory.class);

  @Override
  public ListCell<ProgressBarTask> call(ListView<ProgressBarTask> param) {
    return new ListCell<>() {

      final ProgressBar progressBar = new ProgressBar();
      final Label titleLabel = new Label();
      final VBox contentBox = new VBox(5, titleLabel, progressBar);

      final HBox root = new HBox(10, contentBox);

      {
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        root.setAlignment(Pos.CENTER_LEFT);
      }

      @Override
      public void updateItem(ProgressBarTask progressTask, boolean empty) {
        super.updateItem(progressTask, empty);

        Platform.runLater(() -> {
          if (empty || progressTask == null) {
            setText(null);
            setGraphic(null);
          } else {
            LOG.debug("updating task {} '{}'", progressTask.getTaskId(), progressTask.getTitle());

            StringExpression formattedLabelText = Bindings.format(
                "%s [%d/%d %s]",
                progressTask.titleProperty(),
                progressTask.currentProgressProperty(),
                progressTask.getMaxSize(),
                progressTask.getUnitName()
            );

            titleLabel.textProperty().bind(
                Bindings.when(progressTask.indeterminateProperty())
                    .then(progressTask.titleProperty())
                    .otherwise(formattedLabelText)
            );
            progressBar.progressProperty().bind(
                Bindings.when(progressTask.indeterminateProperty())
                    .then(-1)
                    .otherwise(progressTask.currentProgressProperty().divide((double) progressTask.getMaxSize()))
            );

            //set the size of the progress bar to fill the window completely
            progressBar.setMaxWidth(Double.MAX_VALUE);

            setGraphic(root);
          }
        });
      }
    };
  }
}
