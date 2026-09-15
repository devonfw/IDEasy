package com.devonfw.ide.gui.context;


import com.devonfw.ide.gui.ui.controls.console.ConsoleViewModel;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;

/// Listener class that listens to internal ideasy output, e.g. output from commandlets that are run.
public class IdeGuiLogListener extends IdeLogListenerBuffer {

  private final ConsoleViewModel consoleView;

  /// @param consoleView the console controller to output messages to
  public IdeGuiLogListener(ConsoleViewModel consoleView) {

    super();
    this.consoleView = consoleView;
  }

  @Override
  public boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {

    //If we are in buffer mode, pass responsibility to superclass
    if (this.isBuffering()) {
      super.onLog(level, message, rawMessage, args, error);
    }

    if (this.consoleView != null && message != null) {
      this.consoleView.appendOutput(level, message);
      if (error != null) {
        this.consoleView.appendOutput(IdeLogLevel.ERROR, "  Error: " + error.getMessage());
      }
    }
    return true; // continue processing (also log to standard output if needed)
  }

}
