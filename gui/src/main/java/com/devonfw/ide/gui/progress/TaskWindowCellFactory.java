package com.devonfw.ide.gui.progress;

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

public class TaskWindowCellFactory implements Callback<ListView<GuiProgressBarHandling>, ListCell<GuiProgressBarHandling>> {

  @Override
  public ListCell<GuiProgressBarHandling> call(ListView<GuiProgressBarHandling> param) {
    return new ListCell<>() {
      @Override
      public void updateItem(GuiProgressBarHandling progressTask, boolean empty) {

        super.updateItem(progressTask, empty);
        if (empty || progressTask == null) {
          setText(null);
          setGraphic(null);
          return;
        }

        Label titleLabel = new Label(progressTask.getTitle());

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setProgress((double) progressTask.getCurrentProgress() / progressTask.getMaxSize());

        Button cancelButton = new Button("x");
        cancelButton.setOnAction(event -> {
          progressTask.close(); // oder deine passende Cancel-Methode
        });

        VBox contentBox = new VBox(5, titleLabel, progressBar);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox root = new HBox(10, contentBox, cancelButton);
        root.setAlignment(Pos.CENTER_LEFT);

        setGraphic(root);
      }
    };
  }
}
