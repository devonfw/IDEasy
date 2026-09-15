package com.devonfw.ide.gui.context;

import com.devonfw.ide.gui.ui.controls.console.ConsoleViewModel;

import javafx.application.Platform;

import com.devonfw.ide.gui.ui.controls.console.ConsoleView;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.OutputListener;

/// Listener class that listens to general output from processes and outputs it to the console.
public class GuiOutputListener implements OutputListener {

  private final ConsoleViewModel consoleViewModel;

  /**
   * Constructor.
   *
   * @param consoleViewModel the console view model to output messages to
   */
  public GuiOutputListener(ConsoleViewModel consoleViewModel) {
    this.consoleViewModel = consoleViewModel;
  }

  @Override
  public void onOutput(String message, boolean error) {
    if (this.consoleViewModel != null && message != null) {
      String prefix = error ? "[STDERR] " : "";
      Platform.runLater(() -> this.consoleViewModel.appendOutput(error ? IdeLogLevel.ERROR : IdeLogLevel.INFO, prefix + message));
    }
  }
}
