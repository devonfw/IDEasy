package com.devonfw.tools.ide.cli;

import com.devonfw.tools.ide.process.ProcessResult;

/**
 * {@link CliException} that is thrown if the user aborted further processing due
 */
public final class CliOfflineException extends CliException {

  /**
   * The constructor.
   */
  public CliOfflineException() {

    super("You are offline but network connection is required to perform the operation.", ProcessResult.OFFLINE);
  }

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   */
  public CliOfflineException(String message) {

    super(message, ProcessResult.OFFLINE);
  }

}
