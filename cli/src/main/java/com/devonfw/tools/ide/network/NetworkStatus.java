package com.devonfw.tools.ide.network;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Interface reflecting the current network status in order to determine if we are {@link #isOnline() online} or {@code #isOffline offline}. It also allows to
 * {@link #getError() get the reason} if we are {@code #isOffline offline}.
 */
public interface NetworkStatus extends ReadOfflineMode {

  /**
   * @return {@code true} if {@link #isOfflineMode() offline mode} is active or we are NOT {@link #isOnline() online}, {@code false} otherwise.
   */
  default boolean isOffline() {

    return isOfflineMode() || !isOnline();
  }

  /**
   * @return {@code true} if we are currently online (Internet access is available), {@code false} otherwise.
   */
  boolean isOnline();

  /**
   * @return the {@link Throwable} (typically {@link IOException}) caught on the last online activity, or {@code null} if we are {@link #isOnline() online} or
   *     in {@link #isOfflineMode() offline mode}.
   */
  Throwable getError();

  /**
   * Logs a human-readable message describing the {@link NetworkStatus}.
   * <p>
   * In case of {@link #isOnline() success} this will be a simple success message like "You are online". In case of an {@link #getError() error} an error
   * message should say that you are offline but also include hints for the root cause and a potential solution.
   */
  void logStatusMessage();

  /**
   * Invokes the given {@link Callable} assuming it does some network operation (e.g. download). Further, we assume the {@link Callable} will throw an
   * {@link IOException} in case of a network error and if it throws a {@link RuntimeException} we assume it failed for other reasons. Strictly route all
   * network operations via this method. Be aware that also local file operations may trigger an {@link IOException} so isolate such operations from your
   * {@link Callable}.
   *
   * @param callable the {@link Callable} to {@link Callable#call() invoke}.
   * @param uri the network resource accessed by the {@link Callable}.
   * @param <T> type of the returned value.
   * @return the result of the {@link Callable}.
   * @throws RuntimeException if the {@link Callable} failed.
   */
  <T> T invokeNetworkTask(Callable<T> callable, String uri);

}
