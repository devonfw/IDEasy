package com.devonfw.tools.ide.process;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliProcessException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Implementation of {@link ProcessResult}.
 */
public class ProcessResultImpl implements ProcessResult {

  private final String executable;

  private final String command;

  private final int exitCode;

  private final List<String> out;

  private final List<String> err;

  /**
   * The constructor.
   *
   * @param executable the {@link #getExecutable() executable}.
   * @param command the {@link #getCommand() command}.
   * @param exitCode the {@link #getExitCode() exit code}.
   * @param out the {@link #getOut() out}.
   * @param err the {@link #getErr() err}.
   */
  public ProcessResultImpl(String executable, String command, int exitCode, List<String> out, List<String> err) {

    super();
    this.executable = executable;
    this.command = command;
    this.exitCode = exitCode;
    this.out = Objects.requireNonNullElse(out, Collections.emptyList());
    this.err = Objects.requireNonNullElse(err, Collections.emptyList());
  }

  @Override
  public String getExecutable() {

    return this.executable;
  }

  @Override
  public String getCommand() {

    return this.command;
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
    log(level, context, level);
  }

  @Override
  public void log(IdeLogLevel outLevel, IdeContext context, IdeLogLevel errorLevel) {

    if (!this.out.isEmpty()) {
      doLog(outLevel, this.out, context);
    }
    if (!this.err.isEmpty()) {
      doLog(errorLevel, this.err, context);
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

  @Override
  public void failOnError() throws CliProcessException {

    if (!isSuccessful()) {
      throw new CliProcessException(this);
    }
  }
}
