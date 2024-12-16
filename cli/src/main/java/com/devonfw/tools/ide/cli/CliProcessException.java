package com.devonfw.tools.ide.cli;

import com.devonfw.tools.ide.process.ProcessResult;

/**
 * {@link CliException} that is thrown if a process failed (external tool executed) but we assume this is an error of the end-user.
 *
 * @see com.devonfw.tools.ide.process.ProcessErrorHandling#THROW_CLI
 */
public final class CliProcessException extends CliException {

  private final ProcessResult processResult;

  /**
   * The constructor.
   *
   * @param processResult the {@link #getProcessResult() process result}.
   */
  public CliProcessException(ProcessResult processResult) {

    this("Command " + processResult.getExecutable() + " failed with exit code " + processResult.getExitCode() + " - full commandline was "
        + processResult.getCommand(), processResult);
  }

  /**
   * The constructor.
   *
   * @param message the message to display.
   * @param processResult the {@link #getProcessResult() process result}.
   */
  public CliProcessException(String message, ProcessResult processResult) {

    super(message, processResult.getExitCode());
    this.processResult = processResult;
  }

  /**
   * @return the {@link ProcessResult}.
   */
  public ProcessResult getProcessResult() {

    return this.processResult;
  }
}
