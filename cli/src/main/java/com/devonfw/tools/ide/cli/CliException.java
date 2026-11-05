package com.devonfw.tools.ide.cli;

import com.devonfw.tools.ide.process.ProcessResult;

/**
 * {@link RuntimeException} for to abort CLI process in expected situations. It allows to abort with a defined message for the end user and a defined exit code.
 * Unlike other exceptions a {@link CliException} is not treated as technical error. Therefore by default (unless in debug mode) no stacktrace is printed.
 */
public class CliException extends RuntimeException {

  private final int exitCode;

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   */
  public CliException(String message) {

    this(message, 1);
  }

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   * @param cause the {@link #getCause() cause}.
   */
  public CliException(String message, Throwable cause) {

    this(message, 1, cause);
  }

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   * @param exitCode the {@link #getExitCode() exit code}.
   */
  public CliException(String message, int exitCode) {

    this(message, exitCode, null);
  }

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   * @param exitCode the {@link #getExitCode() exit code}.
   * @param cause the {@link #getCause() cause}.
   */
  public CliException(String message, int exitCode, Throwable cause) {

    super(message, cause);
    assert (exitCode != ProcessResult.SUCCESS);
    this.exitCode = exitCode;
  }

  /**
   * @return the exit code. Should not be zero.
   */
  public int getExitCode() {

    return this.exitCode;
  }

  /**
   * @return {@code true} if this exception has to be re-thrown from a {@link com.devonfw.tools.ide.step.Step Step} even if that {@code Step} was not asked to
   *     re-throw errors, {@code false} otherwise (default). A regular error only makes the according {@code Step} fail while the overall process continues with
   *     the next step. However, if a critical guardrail was violated (e.g. no valid settings could be established) continuing makes no sense and the entire
   *     process has to be aborted.
   */
  public boolean isForceRethrowInStep() {

    return false;
  }

}
