package com.devonfw.ide.gui.core.context;


import com.devonfw.ide.gui.core.mainwindow.console.ConsoleController;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;

/// Listener class that listens to internal ideasy output, e.g. output from commandlets that are run.
public class IdeGuiLogListener extends IdeLogListenerBuffer {

  private final ConsoleController consoleController;

  /// @param consoleController the console controller to output messages to
  public IdeGuiLogListener(ConsoleController consoleController) {

    super();
    this.consoleController = consoleController;
  }

  @Override
  public boolean onLog(IdeLogEntry entry) {

    boolean accept = super.onLog(entry);
    if (accept && (this.consoleController != null)) {
      this.consoleController.appendOutput(entry);
    }
    return accept;
  }

}
