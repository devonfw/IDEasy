package com.devonfw.tools.ide.process;

import java.util.List;

/**
 * Result of a {@link Process} execution.
 *
 * @see ProcessContext#run()
 */
public interface ProcessResult {

  /** Exit code for success. */
  int SUCCESS = 0;

  /**
   * @return the exit code. Will be {@link #SUCCESS} on successful completion of the {@link Process}.
   */
  int getExitCode();

  /**
   * @return {@code true} if the {@link #getExitCode() exit code} indicates {@link #SUCCESS}, {@code false} otherwise
   *         (an error occurred).
   */
  default boolean isSuccessful() {

    return getExitCode() == SUCCESS;
  }

  /**
   * @return the {@link List} with the lines captured on standard out. Will be {@code null} if not captured but
   *         redirected.
   */
  List<String> getOut();

  /**
   * @return the {@link List} with the lines captured on standard error. Will be {@code null} if not captured but
   *         redirected.
   */
  List<String> getErr();

}
