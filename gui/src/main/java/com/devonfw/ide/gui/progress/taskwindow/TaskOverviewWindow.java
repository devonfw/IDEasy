package com.devonfw.ide.gui.progress.taskwindow;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.devonfw.ide.gui.App;
import com.devonfw.ide.gui.context.TaskManager;

/**
 * This window is displayed when the user clicks on the task label in main-view.fxml.
 */
public class TaskOverviewWindow {

  private final Stage stage = new Stage();

  private static TaskOverviewWindow INSTANCE;

  /**
   * @param taskManager the {@link TaskManager} to link to this TaskOverviewWindow.
   * @return instance of the TaskOverviewWindow.
   */
  public static TaskOverviewWindow getInstance(TaskManager taskManager) {

    if (INSTANCE == null) {
      INSTANCE = new TaskOverviewWindow(taskManager);
    }
    return INSTANCE;
  }

  /**
   *
   */
  private TaskOverviewWindow(TaskManager taskManager) {

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("task_overview_window.fxml"));
    fxmlLoader.setController(new TaskOverviewWindowController(taskManager));

    Parent root;
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
  }

  /**
   * display the TaskOverviewWindow (or put it to the front if it is already open).
   */
  public void show() {
    showRelativeToReferenceNode(null);
  }

  /**
   * Displays the dialogue relative to the given reference node.
   *
   * @param referenceNode the node to position the TaskOverviewWindow relative to. If null, the window is centered on the screen.
   */
  public void showRelativeToReferenceNode(Node referenceNode) {

    stage.show();
    Screen screen = Screen.getPrimary();
    double x, y;
    if (referenceNode != null) {
      Point2D referenceNodePos = referenceNode.localToScreen(0, 0);

      x = referenceNodePos.getX() + referenceNode.getBoundsInParent().getWidth() - stage.getWidth();
      y = referenceNodePos.getY() - referenceNode.getBoundsInParent().getHeight() - stage.getHeight();
    } else {
      Rectangle2D screenBounds = screen.getVisualBounds();

      x = screenBounds.getWidth() / 2 - stage.getWidth() / 2;
      y = screenBounds.getHeight() / 2 - stage.getHeight() / 2;
    }
    setPositionRelative(x, y);
  }

  /**
   * @return this TaskOverviewWindow's {@link Stage}.
   */
  public Stage getStage() {
    return stage;
  }

  /**
   * Set the position of the TaskOverviewWindow relative to the given x and y coordinates. The window's bottom right corner is going to be positioned to the top
   * left of the coordinates.
   *
   * @param x x position
   * @param y y position
   */
  public void setPositionRelative(double x, double y) {
    if (stage.isShowing()) {
      stage.setX(x);
      stage.setY(y);
    }
  }
}
