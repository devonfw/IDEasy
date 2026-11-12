package com.devonfw.ide.gui.core.helper;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CoalescingFxRunner}. Instead of the JavaFX thread, a manually drained queue is used as executor, so no JavaFX toolkit is required and the
 * tests are deterministic.
 */
class CoalescingFxRunnerTest extends Assertions {

  private static final long TIMEOUT_SECONDS = 10;

  private final Queue<Runnable> fxQueue = new ConcurrentLinkedQueue<>();

  private final AtomicInteger runs = new AtomicInteger();

  @Test
  void shouldCoalesceRequestsUntilExecuted() {

    CoalescingFxRunner runner = new CoalescingFxRunner(this.runs::incrementAndGet, this.fxQueue::add);

    runner.request();
    runner.request();
    runner.request();

    assertThat(this.fxQueue).hasSize(1);
    assertThat(this.runs).hasValue(0);
    this.fxQueue.poll().run();
    assertThat(this.runs).hasValue(1);
  }

  @Test
  void shouldScheduleAgainAfterExecution() {

    CoalescingFxRunner runner = new CoalescingFxRunner(this.runs::incrementAndGet, this.fxQueue::add);

    runner.request();
    this.fxQueue.poll().run();
    runner.request();

    assertThat(this.fxQueue).hasSize(1);
    this.fxQueue.poll().run();
    assertThat(this.runs).hasValue(2);
  }

  /**
   * A request during the execution of the action may come along with a change the running action has already missed, so it has to schedule another run.
   */
  @Test
  void shouldScheduleAgainWhenRequestedDuringExecution() {

    CoalescingFxRunner[] runner = new CoalescingFxRunner[1];
    runner[0] = new CoalescingFxRunner(() -> {
      if (this.runs.incrementAndGet() == 1) {
        runner[0].request();
      }
    }, this.fxQueue::add);

    runner[0].request();
    this.fxQueue.poll().run();

    assertThat(this.fxQueue).hasSize(1);
    this.fxQueue.poll().run();
    assertThat(this.runs).hasValue(2);
    assertThat(this.fxQueue).isEmpty();
  }

  /**
   * If scheduling fails (e.g. the JavaFX toolkit is not running), the runner must not stay blocked forever.
   */
  @Test
  void shouldAcceptRequestsAgainWhenSchedulingFailed() {

    AtomicInteger attempts = new AtomicInteger();
    CoalescingFxRunner runner = new CoalescingFxRunner(this.runs::incrementAndGet, runnable -> {
      if (attempts.incrementAndGet() == 1) {
        throw new IllegalStateException("Toolkit not initialized");
      }
      this.fxQueue.add(runnable);
    });

    assertThatThrownBy(runner::request).isInstanceOf(IllegalStateException.class);
    runner.request();

    assertThat(this.fxQueue).hasSize(1);
  }

  @Test
  void shouldScheduleOnceForConcurrentRequests() throws InterruptedException {

    CoalescingFxRunner runner = new CoalescingFxRunner(this.runs::incrementAndGet, this.fxQueue::add);
    int threadCount = 8;
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threadCount);
    for (int t = 0; t < threadCount; t++) {
      new Thread(() -> {
        try {
          start.await();
          for (int i = 0; i < 1_000; i++) {
            runner.request();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      }).start();
    }
    start.countDown();

    assertThat(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    assertThat(this.fxQueue).hasSize(1);
  }
}
