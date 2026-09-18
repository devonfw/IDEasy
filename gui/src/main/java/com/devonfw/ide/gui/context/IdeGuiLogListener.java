package com.devonfw.ide.gui.context;


import com.devonfw.ide.gui.event.GuiEventBus;
import com.devonfw.ide.gui.event.console.LogEvent;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;

/// Listener class that listens to internal ideasy output, e.g. output from commandlets that are run.
public class IdeGuiLogListener extends IdeLogListenerBuffer {

  private final GuiEventBus eventBus;

  /// @param eventBus to send the logs to
  public IdeGuiLogListener(GuiEventBus eventBus) {

    super();
    this.eventBus = eventBus;
  }

  @Override
  public boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {

    //If we are in buffer mode, pass responsibility to superclass
    if (this.isBuffering()) {
      super.onLog(level, message, rawMessage, args, error);
    }

    if (this.eventBus != null && message != null) {
      this.eventBus.sendEvent(new LogEvent(new IdeLogEntry(level, message)));
      if (error != null) {
        this.eventBus.sendEvent(new LogEvent(new IdeLogEntry(IdeLogLevel.ERROR, "  Error: " + error.getMessage())));
      }
    }
    return true; // continue processing (also log to standard output if needed)
  }

}
