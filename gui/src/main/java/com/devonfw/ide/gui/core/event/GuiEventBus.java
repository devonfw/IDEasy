package com.devonfw.ide.gui.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.helper.FxHelper;

import io.github.mmm.event.impl.EventBusImpl;

/**
 * Event bus of the GUI. It dispatches on the thread that sends the event and performs no JavaFX-thread marshalling, so it is threading-neutral and can be used
 * for non-GUI logic as well. A listener that touches the scene graph has to hop to the FX thread itself (see {@link FxHelper#runFxSafe(Runnable)}).
 */
public class GuiEventBus extends EventBusImpl {

  private static final Logger LOG = LoggerFactory.getLogger(GuiEventBus.class);

  public GuiEventBus() {

    super((context, error) -> LOG.error("Error while handling event {}: {}", context, error.getMessage(), error));
  }
}
