package com.devonfw.ide.gui.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.helper.FxHelper;

import io.github.mmm.event.impl.EventBusImpl;

public class GuiEventBus extends EventBusImpl {

  private static final Logger LOG = LoggerFactory.getLogger(GuiEventBus.class);

  public GuiEventBus() {

    super((context, error) -> LOG.error("Error while handling event {}: {}", context, error.getMessage(), error));
  }

  @Override
  public void sendEvent(Object event) {

    FxHelper.runFxSafe(() -> super.sendEvent(event));
  }
}
