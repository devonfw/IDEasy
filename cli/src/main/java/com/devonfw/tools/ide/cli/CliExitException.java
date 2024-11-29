package com.devonfw.tools.ide.cli;

import com.devonfw.tools.ide.process.ProcessResult;

/**
 * {@link CliException} Empty exception that is thrown when a required variable is not set.
 */
public class CliExitException extends CliException {

  /**
   * The constructor.
   */
  public CliExitException() {

    super("", ProcessResult.EXIT);
  }
}
