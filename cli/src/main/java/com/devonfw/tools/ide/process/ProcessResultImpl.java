package com.devonfw.tools.ide.process;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

  private final List<OutputMessage> outputMessages;

  /**
   * The constructor.
   *
   * @param executable the {@link #getExecutable() executable}.
   * @param command the {@link #getCommand() command}.
   * @param exitCode the {@link #getExitCode() exit code}.
   * @param output {@link #getOutputMessages() output Messages}.
   */
  public ProcessResultImpl(String executable, String command, int exitCode, List<OutputMessage> outputMessages) {

    super();
    this.executable = executable;
    this.command = command;
    this.exitCode = exitCode;
    this.outputMessages = Objects.requireNonNullElse(outputMessages, Collections.emptyList());
    this.out = this.outputMessages.stream().filter(outputMessage -> !outputMessage.error()).map(OutputMessage::message).collect(Collectors.toList());
    this.err = this.outputMessages.stream().filter(OutputMessage::error).map(OutputMessage::message).collect(Collectors.toList());
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

    return outputMessages.stream().filter(output -> !output.error()).map(OutputMessage::message).collect(Collectors.toList());
  }

  @Override
  public List<String> getErr() {

    return outputMessages.stream().filter(OutputMessage::error).map(OutputMessage::message).collect(Collectors.toList());
  }

  public List<OutputMessage> getOutputMessages() {

    return outputMessages;

  }

  @Override
  public void log(IdeLogLevel level, IdeContext context) {
    log(level, context, level);
  }

  @Override
  public void log(IdeLogLevel outLevel, IdeContext context, IdeLogLevel errorLevel) {

    if (!this.outputMessages.isEmpty()) {
      doLog(outLevel, getOutputMessages().stream().map(OutputMessage::message).collect(Collectors.toList()), context);
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
