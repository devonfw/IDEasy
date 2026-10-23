package com.devonfw.ide.gui.core.context;


import java.util.Objects;

import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.event.console.LogEvent;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;

/// Listener class that listens to internal ideasy output, e.g. output from commandlets that are run.
public class IdeGuiLogListener extends IdeLogListenerBuffer {

  private final GuiEventBus eventBus;

  /// @param eventBus to send the logs to
  public IdeGuiLogListener(GuiEventBus eventBus) {

    super();
    this.eventBus = Objects.requireNonNull(eventBus);
  }

  @Override
  public boolean onLog(IdeLogEntry entry) {

    boolean accept = super.onLog(entry);
    if (accept) {
      this.eventBus.sendEvent(new LogEvent(entry));
    }
    return accept;
  }

}
