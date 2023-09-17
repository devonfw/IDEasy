package com.devonfw.tools.ide.process;

import java.util.List;

/**
 * Implementation of {@link ProcessResult}.
 */
public class ProcessResultImpl implements ProcessResult {

  private final int exitCode;

  private final List<String> out;

  private final List<String> err;

  /**
   * The constructor.
   *
   * @param exitCode the {@link #getExitCode() exit code}.
   * @param out the {@link #getOut() out}.
   * @param err the {@link #getErr() err}.
   */
  public ProcessResultImpl(int exitCode, List<String> out, List<String> err) {

    super();
    this.exitCode = exitCode;
    this.out = out;
    this.err = err;
  }

  @Override
  public int getExitCode() {

    return this.exitCode;
  }

  @Override
  public List<String> getOut() {

    return this.out;
  }

  @Override
  public List<String> getErr() {

    return this.err;
  }

}
