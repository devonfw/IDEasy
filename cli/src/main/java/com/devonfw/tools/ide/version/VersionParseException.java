package com.devonfw.tools.ide.version;

import com.devonfw.tools.ide.cli.CliException;

/**
 * Exception thrown if a version identifier cannot be parsed.
 */
public class VersionParseException extends CliException {

  /**
   * The constructor.
   *
   * @param message the error message.
   */
  public VersionParseException(String message) {

    super(message);
  }
}
