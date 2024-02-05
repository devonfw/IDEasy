package com.devonfw.tools.ide.util;

import java.util.List;

import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Mocks a process result with adjustable params
 */
public class GitUtilsProcessResultMock implements ProcessResult {

  private final List<String> outs;

  private final List<String> errors;

  private final int exitCode;

  /**
   * 
   * @param errors List of errors.
   * @param outs List of out texts.
   * @param exitCode the exit code.
   */
  public GitUtilsProcessResultMock(List<String> errors, List<String> outs, int exitCode) {

    this.errors = errors;
    this.outs = outs;
    this.exitCode = exitCode;
  }

  @Override
  public int getExitCode() {

    return this.exitCode;
  }

  @Override
  public List<String> getOut() {

    return this.outs;
  }

  @Override
  public List<String> getErr() {

    return this.errors;
  }
}
