package com.devonfw.ide.gui.core.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Pins the threading contract of the {@link GuiEventBus}: it dispatches events synchronously on the thread that sends them and performs no JavaFX-thread
 * marshalling, so non-GUI threads (e.g. the CLI/worker threads that will route logs to the GUI console) can publish without blocking on or hopping to the FX
 * thread. Sending from multiple threads concurrently is safe: dispatching is serialized, so no event is lost and listeners never run concurrently.
 */
public class GuiEventBusTest extends Assertions {

  private static final long TIMEOUT_SECONDS = 10;

  private record SomeEvent() {

  }

  private record BlockingEvent() {

  }

  private record OuterEvent() {

  }

  private record InnerEvent() {

  }

  private record CountedEvent(int id) {

  }

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

  /**
   * An event sent while another thread is still dispatching must not be silently queued for (and later delivered by) that other thread. Instead, the sender
   * has to wait until the running dispatch is finished and then dispatch its own event synchronously on its own thread.
   */
  @Test
  public void testEventSentDuringForeignDispatchIsDeliveredOnSendingThreadBeforeSendReturns() throws InterruptedException {

    GuiEventBus eventBus = new GuiEventBus();
    CountDownLatch dispatchEntered = new CountDownLatch(1);
    CountDownLatch releaseDispatch = new CountDownLatch(1);
    eventBus.addListener(BlockingEvent.class, event -> {
      dispatchEntered.countDown();
      await(releaseDispatch);
    });
    AtomicReference<Thread> listenerThread = new AtomicReference<>();
    eventBus.addListener(SomeEvent.class, event -> listenerThread.set(Thread.currentThread()));

    Thread dispatcher = new Thread(() -> eventBus.sendEvent(new BlockingEvent()), "dispatcher");
    dispatcher.start();
    assertThat(dispatchEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

    AtomicReference<Thread> listenerThreadWhenSendReturned = new AtomicReference<>();
    Thread sender = new Thread(() -> {
      eventBus.sendEvent(new SomeEvent());
      listenerThreadWhenSendReturned.set(listenerThread.get());
    }, "sender");
    sender.start();
    // the sender either has to block on the running dispatch (expected) or returns early (broken thread-safety)
    while (sender.isAlive() && (sender.getState() != Thread.State.BLOCKED)) {
      Thread.onSpinWait();
    }

    releaseDispatch.countDown();
    dispatcher.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
    sender.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

    assertThat(listenerThreadWhenSendReturned.get())
        .as("The event must have been delivered on the sending thread before sendEvent returned")
        .isSameAs(sender);
  }

  /**
   * An event sent from within a listener must be delivered after the current listener has completed (no nested dispatch), but still on the same thread and
   * before the outer sendEvent returns.
   */
  @Test
  public void testEventSentFromListenerIsDeliveredAfterCurrentListenerOnSameThread() {

    GuiEventBus eventBus = new GuiEventBus();
    List<String> log = new CopyOnWriteArrayList<>();
    eventBus.addListener(OuterEvent.class, event -> {
      log.add("outer-start");
      eventBus.sendEvent(new InnerEvent());
      log.add("outer-end");
    });
    eventBus.addListener(InnerEvent.class, event -> log.add("inner@" + Thread.currentThread().getName()));

    eventBus.sendEvent(new OuterEvent());

    assertThat(log).containsExactly("outer-start", "outer-end", "inner@" + Thread.currentThread().getName());
  }

  /**
   * Many threads sending concurrently: every event has to be delivered exactly once and listeners must never run concurrently.
   */
  @Test
  public void testConcurrentSendersDeliverEveryEventExactlyOnceWithoutConcurrentListeners() throws InterruptedException {

    int threadCount = 8;
    int eventsPerThread = 2_000;
    GuiEventBus eventBus = new GuiEventBus();
    AtomicInteger activeListeners = new AtomicInteger();
    AtomicInteger maxActiveListeners = new AtomicInteger();
    AtomicInteger deliveredEvents = new AtomicInteger();
    eventBus.addListener(CountedEvent.class, event -> {
      maxActiveListeners.accumulateAndGet(activeListeners.incrementAndGet(), Math::max);
      deliveredEvents.incrementAndGet();
      Thread.yield();
      activeListeners.decrementAndGet();
    });

    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threadCount);
    for (int t = 0; t < threadCount; t++) {
      int offset = t * eventsPerThread;
      Thread sender = new Thread(() -> {
        await(start);
        for (int i = 0; i < eventsPerThread; i++) {
          eventBus.sendEvent(new CountedEvent(offset + i));
        }
        done.countDown();
      }, "sender-" + t);
      sender.start();
    }
    start.countDown();

    assertThat(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    assertThat(deliveredEvents.get()).isEqualTo(threadCount * eventsPerThread);
    assertThat(maxActiveListeners.get()).as("listeners must never run concurrently").isEqualTo(1);
  }

  private static void await(CountDownLatch latch) {

    try {
      if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timeout while waiting for latch");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
