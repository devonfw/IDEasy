package com.devonfw.ide.gui.context;

import javafx.application.Platform;

import com.devonfw.ide.gui.console.ConsoleController;
import com.devonfw.tools.ide.process.OutputListener;

public class GuiOutputListener implements OutputListener {

  private final ConsoleController consoleController;

  /**
   * Constructor.
   *
   * @param consoleController the console controller to output messages to
   */
  public GuiOutputListener(ConsoleController consoleController) {
    this.consoleController = consoleController;
  }

  @Override
  public void onOutput(String message, boolean error) {
    if (this.consoleController != null && message != null) {
      String prefix = error ? "[STDERR] " : "";
      Platform.runLater(() -> this.consoleController.appendOutput(prefix + message));
    }
  }
}
