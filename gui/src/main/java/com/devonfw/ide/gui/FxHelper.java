package com.devonfw.ide.gui;

import javafx.application.Platform;
import javafx.scene.Parent;

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

  /// @param root root node
  /// @param selector id of node to be selected
  /// @param <T> inferred type of the node
  /// @return Node in inferred type
  @SuppressWarnings("unchecked")
  public static <T> T lookup(Parent root, String selector) {

    return (T) root.lookup(selector);
  }
}
