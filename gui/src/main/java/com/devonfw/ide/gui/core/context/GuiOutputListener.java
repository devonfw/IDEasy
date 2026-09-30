package com.devonfw.ide.gui.context;

import javafx.application.Platform;

import com.devonfw.ide.gui.event.GuiEventBus;
import com.devonfw.ide.gui.event.console.LogEvent;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.OutputListener;

/// Listener class that listens to general output from processes and outputs it to the console.
public class GuiOutputListener implements OutputListener {

  private final GuiEventBus eventBus;

  /**
   * Constructor.
   *
   * @param eventBus the event bus to send log events to
   */
  public GuiOutputListener(GuiEventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void onOutput(String message, boolean error) {
    if (this.eventBus != null && message != null) {
      String prefix = error ? "[STDERR] " : "";
      Platform.runLater(() -> this.eventBus.sendEvent(new LogEvent(new IdeLogEntry(error ? IdeLogLevel.ERROR : IdeLogLevel.INFO, prefix + message))));
    }
  }
}
