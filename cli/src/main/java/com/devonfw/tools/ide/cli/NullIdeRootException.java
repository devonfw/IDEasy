package com.devonfw.tools.ide.cli;

/**
 * {@link CliException} that is thrown if IDE_ROOT == null
 */
public class NullIdeRootException extends CliException {

  /**
   * The constructor.
   */
  public NullIdeRootException() {

    super("");
  }
}
