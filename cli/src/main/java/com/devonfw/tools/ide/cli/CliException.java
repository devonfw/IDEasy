package com.devonfw.tools.ide.cli;

/**
 * {@link RuntimeException} for to abort CLI process in expected situations. It allows to abort with a defined message
 * for the end user and a defined exit code. Unlike other exceptions a {@link CliException} is not treated as technical
 * error. Therefore by default (unless in debug mode) no stacktrace is printed.
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

    super(message);
    this.exitCode = exitCode;
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
    this.exitCode = exitCode;
  }

  /**
   * @return the exit code. Should not be zero.
   */
  public int getExitCode() {

    return this.exitCode;
  }

}
