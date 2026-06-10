package com.devonfw.ide.gui;

import javafx.application.Platform;

/**
 * Helper class containing tools for interacting with JavaFX
 */
public class FxHelper {

  /**
   * Allows to run operations on the Fx Application Thread, but only, if the call is originating from the Fx Application Thread. Idea: Some tasks which are
   * doing (potentially heavy) background work, might not originate from the Fx Application (UI) Thread.
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

}
