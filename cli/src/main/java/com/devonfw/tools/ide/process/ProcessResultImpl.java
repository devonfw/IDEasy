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

  private final List<OutputMessage> outputMessages;

  /**
   * The constructor.
   *
   * @param executable the {@link #getExecutable() executable}.
   * @param command the {@link #getCommand() command}.
   * @param exitCode the {@link #getExitCode() exit code}.
   * @param outputMessages {@link #getOutputMessages() output Messages}.
   */
  public ProcessResultImpl(String executable, String command, int exitCode, List<OutputMessage> outputMessages) {

    super();
    this.executable = executable;
    this.command = command;
    this.exitCode = exitCode;
    this.outputMessages = Objects.requireNonNullElse(outputMessages, Collections.emptyList());
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

    return this.outputMessages.stream().filter(outputMessage -> !outputMessage.error()).map(OutputMessage::message).toList();
  }

  @Override
  public List<String> getErr() {

    return this.outputMessages.stream().filter(OutputMessage::error).map(OutputMessage::message).toList();
  }

  @Override
  public List<OutputMessage> getOutputMessages() {

    return this.outputMessages;

  }

  @Override
  public void log(IdeLogLevel level, IdeContext context) {
    log(level, context, level);
  }

  @Override
  public void log(IdeLogLevel outLevel, IdeContext context, IdeLogLevel errorLevel) {

    if (!this.outputMessages.isEmpty()) {
      for (OutputMessage outputMessage : this.outputMessages) {
        if (outputMessage.error()) {
          doLog(errorLevel, outputMessage.message(), context);
        } else {
          doLog(outLevel, outputMessage.message(), context);
        }
      }
    }
  }

  private void doLog(IdeLogLevel level, String message, IdeContext context) {
    // remove !MESSAGE from log message
    if (message.startsWith("!MESSAGE ")) {
      message = message.substring(9);
    }
    context.level(level).log(message);
  }

  @Override
  public void failOnError() throws CliProcessException {

    if (!isSuccessful()) {
      throw new CliProcessException(this);
    }
  }
}
