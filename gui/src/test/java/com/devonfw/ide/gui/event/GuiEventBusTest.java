package com.devonfw.ide.gui.event;

import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Pins the threading contract of the {@link GuiEventBus}: it dispatches events synchronously on the thread that sends them and performs no JavaFX-thread
 * marshalling, so non-GUI threads (e.g. the CLI/worker threads that will route logs to the GUI console) can publish without blocking on or hopping to the FX
 * thread.
 */
public class GuiEventBusTest extends Assertions {

  private record SomeEvent() {}

  /**
   * An event sent from a plain (non-FX) thread must reach its listener synchronously, on that very same thread.
   */
  @Test
  public void testEventIsDispatchedSynchronouslyOnTheSendingThread() {

    GuiEventBus eventBus = new GuiEventBus();
    AtomicReference<Thread> listenerThread = new AtomicReference<>();
    eventBus.addListener(SomeEvent.class, event -> listenerThread.set(Thread.currentThread()));

    Thread sender = Thread.currentThread();
    eventBus.sendEvent(new SomeEvent());

    assertThat(listenerThread.get())
        .as("The listener must have run before sendEvent returned, on the thread that sent the event")
        .isSameAs(sender);
  }
}
