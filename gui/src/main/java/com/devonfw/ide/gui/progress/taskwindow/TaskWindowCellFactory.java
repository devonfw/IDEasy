package com.devonfw.ide.gui.progress.taskwindow;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import com.devonfw.ide.gui.progress.GuiProgressBarHandling;

public class TaskWindowCellFactory implements Callback<ListView<GuiProgressBarHandling>, ListCell<GuiProgressBarHandling>> {

  @Override
  public ListCell<GuiProgressBarHandling> call(ListView<GuiProgressBarHandling> param) {
    return new ListCell<>() {

      final ProgressBar progressBar = new ProgressBar();
      final Button cancelButton = new Button("x");

      @Override
      public void updateItem(GuiProgressBarHandling progressTask, boolean empty) {

        progressBar.progressProperty().unbind();

        super.updateItem(progressTask, empty);
        if (empty || progressTask == null) {

          setText(null);
          setGraphic(null);
        } else {

          Label titleLabel = new Label(progressTask.getTitle());

          progressBar.setMaxWidth(Double.MAX_VALUE);
          progressBar.progressProperty().bind(progressTask.progressProperty());

          cancelButton.setOnAction(event -> {
            progressTask.close(); // oder deine passende Cancel-Methode
          });

          VBox contentBox = new VBox(5, titleLabel, progressBar);
          HBox.setHgrow(contentBox, Priority.ALWAYS);

          HBox root = new HBox(10, contentBox, cancelButton);
          root.setAlignment(Pos.CENTER_LEFT);

          setGraphic(root);
        }
      }
    };
  }
}
