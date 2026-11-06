package com.devonfw.ide.gui.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.helper.FxHelper;

import io.github.mmm.event.impl.EventBusImpl;

/**
 * Event bus of the GUI. It dispatches on the thread that sends the event and performs no JavaFX-thread marshalling, so it is threading-neutral and can be used
 * for non-GUI logic as well. A listener that touches the scene graph has to hop to the FX thread itself (see {@link FxHelper#runFxSafe(Runnable)}).
 * <p>
 * Sending events is thread-safe: dispatching is serialized, so every event is delivered exactly once, synchronously on its sending thread before
 * {@link #sendEvent(Object)} returns, and listeners never run concurrently. As a consequence, a sender blocks while another thread is dispatching, so listeners
 * have to be short and must never wait for another thread that may send an event itself (deadlock). An event sent from within a listener is delivered after
 * that listener has completed.
 */
public class GuiEventBus extends EventBusImpl {

  private static final Logger LOG = LoggerFactory.getLogger(GuiEventBus.class);

  public GuiEventBus() {

    super((context, error) -> LOG.error("Error while handling event {}: {}", context, error.getMessage(), error));
  }

  /**
   * {@link EventBusImpl} is not thread-safe: its re-entrance guard ({@code dispatching} flag) is checked and set non-atomically. Concurrent senders could
   * therefore dispatch in parallel, or enqueue an event that is delivered later by a foreign thread or even remains stuck in the queue until the next event is
   * sent. Making enqueue and dispatch one atomic step fixes this. Re-entrant sends from a listener still work, as the monitor is re-entrant and the inherited
   * flag defers the nested event until the current listener has completed.
   */
  @Override
  public synchronized void sendEvent(Object event) {

    super.sendEvent(event);
  }
}
