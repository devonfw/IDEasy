package com.devonfw.ide.gui.progress.taskwindow;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.devonfw.ide.gui.App;

/**
 * This window is displayed when the user clicks on the task label in main-view.fxml.
 */
public class TaskOverviewWindow {

  private final Parent root;
  private final Stage stage = new Stage();

  private static TaskOverviewWindow INSTANCE;

  /**
   * @param referenceNode reference node to determine the position of the TaskOverviewWindow.
   * @return instance of the TaskOverviewWindow.
   */
  public static TaskOverviewWindow getInstance(Node referenceNode) {
    if (INSTANCE == null) {
      Point2D point = referenceNode.localToScreen(0, 0);

      double nodeRightEdge = point.getX() + referenceNode.getBoundsInLocal().getWidth();
      double nodeTopEdge = point.getY() - referenceNode.getBoundsInLocal().getHeight();

      INSTANCE = new TaskOverviewWindow(nodeRightEdge, nodeTopEdge);
      return INSTANCE;
    }
    return INSTANCE;
  }

  /**
   *
   */
  private TaskOverviewWindow(Double x, Double y) {

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("task_overview_window.fxml"));

    try {
      root = fxmlLoader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    stage.setResizable(false);
    stage.setAlwaysOnTop(true);
    stage.setTitle("Running tasks");
    stage.setScene(new Scene(root));
    stage.initStyle(StageStyle.UTILITY);

    stage.setOnShown(event -> {
      stage.setX(x - stage.getScene().getWidth());
      stage.setY(y - stage.getScene().getHeight());
    });
  }

  /**
   * display the TaskOverviewWindow (or put it to the front if it is already open).
   */
  public void show() {
    stage.show();
    stage.requestFocus();
  }
}
