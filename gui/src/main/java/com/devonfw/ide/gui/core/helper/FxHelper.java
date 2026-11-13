package com.devonfw.ide.gui.core.helper;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

import com.devonfw.ide.gui.core.service.NlsService;

/**
 * Helper class containing tools for interacting with JavaFX
 */
public class FxHelper {

  /**
   * Allows running operations on the Fx Application Thread, but only if the call is originating from the Fx Application Thread. Idea: Some tasks that are doing
   * (potentially heavy) background work might not originate from the Fx Application (UI) Thread but will still interact with javafx observable collections.
   *
   * @param runnable code to execute
   */
  public static void runFxSafe(Runnable runnable) {
    if (Platform.isFxApplicationThread()) {
      runnable.run();
    } else {
      Platform.runLater(runnable);
    }
  }

  /**
   * Runs the given task on a new daemon background thread and afterwards runs the given callback on the Fx Application Thread.
   *
   * @param task the work to execute in the background.
   * @param onFinished code to execute on the Fx Application Thread once {@code task} has completed.
   */
  public static void runInBackground(Runnable task, Runnable onFinished) {

    Thread thread = new Thread(() -> {
      task.run();
      Platform.runLater(onFinished);
    });
    thread.setDaemon(true);
    thread.start();
  }

  /// @param root root node
  /// @param selector id of node to be selected
  /// @param <T> inferred type of the node
  /// @return Node in inferred type
  @SuppressWarnings("unchecked")
  public static <T> T lookup(Parent root, String selector) {

    return (T) root.lookup(selector);
  }

  /**
   * Loads the given FXML file into the given root node, using the {@link NlsService}'s resource bundle to resolve the {@code %key} text.
   *
   * @param fxmlName the FXML file name, resolved relative to the root node's class.
   * @param nlsService the localization service whose resource bundle resolves the text keys.
   * @param root the node that is both the FXML root and its controller.
   */
  public static void loadFxml(String fxmlName, NlsService nlsService, Node root) {
    final FXMLLoader loader = new FXMLLoader(root.getClass().getResource(fxmlName));
    loader.setRoot(root);
    loader.setController(root);
    loader.setResources(nlsService.getResourceBundle());
    try {
      loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
