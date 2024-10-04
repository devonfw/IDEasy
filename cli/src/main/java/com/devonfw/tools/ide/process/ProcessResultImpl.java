package com.devonfw.tools.ide.process;

import java.util.Collections;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

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
    if (out == null) {
      this.out = Collections.emptyList();
    } else {
      this.out = out;
    }
    if (err == null) {
      this.err = Collections.emptyList();
    } else {
      this.err = err;
    }
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

  @Override
  public void log(IdeLogLevel level, IdeContext context) {

    if (this.out != null && !this.out.isEmpty() && level == IdeLogLevel.INFO) {
      doLog(level, this.out, context);
    }
    if (this.err != null && !this.err.isEmpty() && level == IdeLogLevel.ERROR) {
      doLog(level, this.err, context);
    }
  }

  private void doLog(IdeLogLevel level, List<String> lines, IdeContext context) {
    for (String line : lines) {
      // remove !MESSAGE from log message
      if (line.startsWith("!MESSAGE ")) {
        line = line.substring(9);
      }
      context.level(level).log(line);
    }
  }

}
