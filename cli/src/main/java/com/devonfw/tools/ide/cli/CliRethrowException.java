package com.devonfw.tools.ide.cli;


/**
 * {@link CliException} that is thrown to immediately abort the CLI process when a critical guardrail fails
 * (e.g., settings repository cannot be cloned or validated). This ensures the process stops rather than
 * continuing in an invalid state.
 */
public final class CliRethrowException extends CliException {

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   */
  public CliRethrowException(String message) {

    super(message);
  }

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   * @param cause the {@link #getCause() cause}.
   */
  public CliRethrowException(String message, Throwable cause) {

    super(message, cause);
  }
}
