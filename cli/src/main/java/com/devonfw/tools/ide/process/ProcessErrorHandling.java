package com.devonfw.tools.ide.process;

/**
 * {@link Enum} with the available handling if a {@link Process#exitValue() status code} was not
 * {@link ProcessResult#SUCCESS successful}.
 */
public enum ProcessErrorHandling {

  /**
   * Always just return the status code without reacting to failure. When using this, you should handle the exit code
   * carefully yourself.
   */
  NONE,

  /** Log a warning if the status code was not successful. */
  WARNING,

  /** Log an error if the status code was not successful. */
  ERROR,

  /**
   * Throw an exception if the status code was not successful. In this case the {@link ProcessContext#run() run} method
   * will never return an exit code other than {@link ProcessResult#SUCCESS} as otherwise an exception is thrown
   * preventing the method to return.
   */
  THROW

}
