package com.devonfw.ide.gui.progress.taskwindow;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.devonfw.ide.gui.App;

/**
 * This window is displayed when the user clicks on the task label in main-view.fxml.
 */
public class TaskOverviewWindow {

  private Parent root;

  /**
   *
   */
  public TaskOverviewWindow() {

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("task_overview_window.fxml"));

    try {
      root = fxmlLoader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Stage stage = new Stage();
    stage.setResizable(false);
    stage.setAlwaysOnTop(true);
    stage.setTitle("Running tasks");
    stage.setScene(new Scene(root));
    stage.initStyle(StageStyle.UTILITY);
    stage.show();
  }
}
