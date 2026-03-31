package com.devonfw.tools.ide.process;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliProcessException;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Implementation of {@link ProcessResult}.
 */
public class ProcessResultImpl implements ProcessResult {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessResultImpl.class);

  private final String executable;

  private final String command;

  private final int exitCode;

  private final List<OutputMessage> outputMessages;

  private final boolean success;

  /**
   * The constructor.
   *
   * @param executable the {@link #getExecutable() executable}.
   * @param command the {@link #getCommand() command}.
   * @param exitCode the {@link #getExitCode() exit code}.
   * @param outputMessages {@link #getOutputMessages() output Messages}.
   */
  public ProcessResultImpl(String executable, String command, int exitCode, List<OutputMessage> outputMessages) {
    this(executable, command, exitCode, exitCode == SUCCESS, outputMessages);
  }

  /**
   * The constructor.
   *
   * @param executable the {@link #getExecutable() executable}.
   * @param command the {@link #getCommand() command}.
   * @param exitCode the {@link #getExitCode() exit code}.
   * @param success the {@link #isSuccessful() success} flag.
   * @param outputMessages {@link #getOutputMessages() output Messages}.
   */
  public ProcessResultImpl(String executable, String command, int exitCode, boolean success, List<OutputMessage> outputMessages) {

    super();
    this.executable = executable;
    this.command = command;
    this.exitCode = exitCode;
    this.success = success;
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
  public String getSingleOutput(IdeLogLevel logLevel) throws IllegalStateException {
    String errorMessage;
    if (this.isSuccessful()) {
      List<String> out = this.getOut();
      int size = out.size();
      if (size == 1) {
        return out.getFirst();
      } else if (size == 0) {
        errorMessage = "No output received from " + this.getCommand();
      } else {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected single line of output but received ");
        sb.append(size);
        sb.append(" lines from ");
        sb.append(this.getCommand());
        sb.append(":");
        for (String line : out) {
          sb.append("\n");
          sb.append(line);
        }
        errorMessage = sb.toString();
      }
    } else {
      errorMessage = "Command " + this.getCommand() + " failed with exit code " + this.getExitCode();
    }
    if (logLevel == null) {
      throw new IllegalStateException(errorMessage);
    } else {
      doLog(logLevel, errorMessage);
      return null;
    }
  }

  @Override
  public List<String> getOutput(IdeLogLevel logLevel) throws IllegalStateException {
    String errorMessage;
    if (this.isSuccessful()) {
      List<String> out = this.getOut();
      return out;
    } else {
      errorMessage = "Command " + this.getCommand() + " failed with exit code " + this.getExitCode();
    }
    if (logLevel == null) {
      throw new IllegalStateException(errorMessage);
    } else {
      doLog(logLevel, errorMessage);
      return null;
    }
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
  public boolean isSuccessful() {

    return this.success;
  }

  @Override
  public void log(IdeLogLevel level) {
    log(level, level);
  }

  @Override
  public void log(IdeLogLevel outLevel, IdeLogLevel errorLevel) {

    if (!this.outputMessages.isEmpty()) {
      for (OutputMessage outputMessage : this.outputMessages) {
        if (outputMessage.error()) {
          doLog(errorLevel, outputMessage.message());
        } else {
          doLog(outLevel, outputMessage.message());
        }
      }
    }
  }

  private void doLog(IdeLogLevel level, String message) {
    // remove !MESSAGE from log message
    if (message.startsWith("!MESSAGE ")) {
      message = message.substring(9);
    }
    LOG.atLevel(level.getSlf4jLevel()).log(message);
  }

  @Override
  public void failOnError() throws CliProcessException {

    if (!isSuccessful()) {
      throw new CliProcessException(this);
    }
  }
}
