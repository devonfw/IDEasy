package com.devonfw.ide.gui.progress.taskwindow;

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

import com.devonfw.ide.gui.progress.GuiProgressBarHandling;

/**
 * Cell factory for displaying a list of tasks in the {@link TaskOverviewWindow}
 */
public class TaskWindowCellFactory implements Callback<ListView<GuiProgressBarHandling>, ListCell<GuiProgressBarHandling>> {

  private static final Logger LOG = LoggerFactory.getLogger(TaskWindowCellFactory.class);

  @Override
  public ListCell<GuiProgressBarHandling> call(ListView<GuiProgressBarHandling> param) {
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
      public void updateItem(GuiProgressBarHandling progressTask, boolean empty) {

        if (empty || progressTask == null) {
          setText(null);
          setGraphic(null);
        } else {
          LOG.debug("update cell");

          titleLabel.setText(
              getLabelValueFormatted(progressTask.getTitle(), progressTask.getCurrentProgress(), progressTask.getMaxSize(), progressTask.getUnitName()));

          if (progressTask.isIndeterminate() && !progressBar.isIndeterminate()) {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
          } else if (!progressTask.isIndeterminate()) {
            progressBar.setProgress((double) progressTask.getCurrentProgress() / progressTask.getMaxSize());
          }

          progressBar.setMaxWidth(Double.MAX_VALUE);

          setGraphic(root);
        }
      }
    };
  }

  private String getLabelValueFormatted(String title, long _currentProgress, long maxSize, String unitName) {
    return String.format("%s [%d/%d %s]", title, _currentProgress, maxSize, unitName);
  }
}
