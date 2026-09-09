package com.devonfw.ide.gui.context;

import javafx.application.Platform;

import com.devonfw.ide.gui.console.ConsoleController;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.OutputListener;

/// Listener class that listens to general output from processes and outputs it to the console.
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
      Platform.runLater(() -> this.consoleController.appendOutput(error ? IdeLogLevel.ERROR : IdeLogLevel.INFO, prefix + message));
    }
  }
}
