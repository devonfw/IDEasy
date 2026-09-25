package com.devonfw.ide.gui.update;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.concurrent.Task;

/**
 * Shared helper to run blocking work off the JavaFX Application Thread and marshal the result (or failure) back onto it.
 */
final class TasksHelper {

  private TasksHelper() {

  }

  /**
   * Runs {@code work} on a new daemon thread and invokes {@code onSuccess} or {@code onFailure} on the JavaFX Application Thread once it completes.
   *
   * @param <T> the result type of the work.
   * @param work the blocking work to perform off the FX thread.
   * @param onSuccess callback invoked on the FX thread with the result if {@code work} completes normally.
   * @param onFailure callback invoked on the FX thread with the exception if {@code work} throws.
   * @param threadName the name of the background thread (for diagnostics).
   */
  static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure, String threadName) {

    Task<T> task = new Task<>() {

      @Override
      protected T call() {
        return work.get();
      }
    };

    task.setOnSucceeded(ignored -> onSuccess.accept(task.getValue()));
    task.setOnFailed(ignored -> onFailure.accept(task.getException()));

    Thread thread = new Thread(task, threadName);
    thread.setDaemon(true);
    thread.start();
  }
}
