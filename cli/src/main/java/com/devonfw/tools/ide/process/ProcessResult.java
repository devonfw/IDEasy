package com.devonfw.tools.ide.process;

import java.util.List;

/**
 * Result of a {@link Process} execution.
 *
 * @see ProcessContext#run()
 */
public interface ProcessResult {

  /** Return code for success. */
  int SUCCESS = 0;

  /** Return code if tool was requested that is not installed. */
  int TOOL_NOT_INSTALLED = 4;

  /**
   * Return code to abort gracefully.
   *
   * @see com.devonfw.tools.ide.cli.CliAbortException
   */
  int ABORT = 22;

  /**
   * Return code if {@link com.devonfw.tools.ide.context.IdeContext#isOffline() offline} but network is required for
   * requested operation.
   *
   * @see com.devonfw.tools.ide.cli.CliOfflineException
   */
  int OFFLINE = 23;

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
