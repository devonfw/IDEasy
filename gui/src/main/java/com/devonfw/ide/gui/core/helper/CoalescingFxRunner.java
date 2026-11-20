package com.devonfw.ide.gui.core.helper;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;

/**
 * Runs an action on the JavaFX thread on request, coalescing all requests that arrive while a run is still pending into that single run. This keeps a flood of
 * requests (e.g. one per progress step of a download) from flooding the JavaFX thread. The action should therefore pull the current state instead of relying
 * on individual requests. {@link #request()} is thread-safe and never runs the action inline, so it can be called from any thread, also while holding locks.
 */
public class CoalescingFxRunner {

  private final Runnable action;

  private final Executor fxExecutor;

  private final AtomicBoolean pending = new AtomicBoolean();

  /**
   * @param action the action to run on the JavaFX thread.
   */
  public CoalescingFxRunner(Runnable action) {

    this(action, Platform::runLater);
  }

  /**
   * @param action the action to run.
   * @param fxExecutor the {@link Executor} to run the action with (for testing without a JavaFX toolkit).
   */
  CoalescingFxRunner(Runnable action, Executor fxExecutor) {

    this.action = Objects.requireNonNull(action);
    this.fxExecutor = Objects.requireNonNull(fxExecutor);
  }

  /**
   * Requests a run of the action. Does nothing if a run is already pending.
   */
  public void request() {

    if (this.pending.compareAndSet(false, true)) {
      try {
        this.fxExecutor.execute(this::run);
      } catch (RuntimeException e) {
        // nothing was scheduled (e.g. JavaFX toolkit not running), so do not block further requests forever
        this.pending.set(false);
        throw e;
      }
    }
  }

  private void run() {

    // reset before running, so that a request during the action (that may come along with a change the action has already missed) schedules another run
    this.pending.set(false);
    this.action.run();
  }
}
