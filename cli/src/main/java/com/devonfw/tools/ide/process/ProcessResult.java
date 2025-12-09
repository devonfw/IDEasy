package com.devonfw.tools.ide.process;

import java.util.List;

import com.devonfw.tools.ide.cli.CliProcessException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;

/**
 * Result of a {@link Process} execution.
 *
 * @see ProcessContext#run()
 */
public interface ProcessResult {

  /** Return code for success. */
  int SUCCESS = 0;

  /** Return code if IDE_HOME is required but not found. */
  int NO_IDE_HOME = 2;

  /** Return code if IDE_ROOT is required but not found. */
  int NO_IDE_ROOT = 3;

  /** Return code if tool was requested that is not installed. */
  int TOOL_NOT_INSTALLED = 4;

  /** Return code to exit if condition not met */
  int EXIT = 17;

  /**
   * Return code to abort gracefully.
   *
   * @see com.devonfw.tools.ide.cli.CliAbortException
   */
  int ABORT = 22;

  /**
   * Return code if {@link com.devonfw.tools.ide.context.IdeContext#isOffline() offline} but network is required for requested operation.
   *
   * @see com.devonfw.tools.ide.cli.CliOfflineException
   */
  int OFFLINE = 23;

  /**
   * @return the filename of the executable that was run (e.g. "git").
   * @see #getCommand()
   */
  String getExecutable();

  /**
   * @return the full command that was executed (e.g. "git rev-parse HEAD").
   */
  String getCommand();

  /**
   * @return the exit code. Will be {@link #SUCCESS} on successful completion of the {@link Process}.
   */
  int getExitCode();

  /**
   * @return {@code true} if the {@link #getExitCode() exit code} indicates {@link #SUCCESS}, {@code false} otherwise (an error occurred).
   */
  default boolean isSuccessful() {

    return getExitCode() == SUCCESS;
  }

  /**
   * @return the first captured standard out. Will be {@code null} if not captured but redirected.
   */
  default String getSingleOutput() {

    return getSingleOutput(null);
  }

  /**
   * @param logger the {@link IdeSubLogger logger} to use.
   * @return the first captured standard out. Will be {@code null} if not captured but redirected.
   */
  String getSingleOutput(IdeSubLogger logger);

  /**
   * @param logger the {@link IdeSubLogger logger} to use.
   * @return the first captured standard out. Will be {@code null} if not captured but redirected.
   */
  List<String> getOutput(IdeSubLogger logger);

  /**
   * @return the {@link List} with the lines captured on standard out. Will be {@code null} if not captured but redirected.
   */
  List<String> getOut();

  /**
   * @return the {@link List} with the lines captured on standard error. Will be {@code null} if not captured but redirected.
   */
  List<String> getErr();

  /**
   * @return the {@link List} with {@link OutputMessage} that captured on standard out and standard error lines. Will be {@code null} if not captured but
   *     redirected.
   */
  List<OutputMessage> getOutputMessages();

  /**
   * Logs output and error messages on the provided log level.
   *
   * @param level the {@link IdeLogLevel} to use e.g. IdeLogLevel.ERROR.
   * @param context the {@link IdeContext} to use.
   */
  void log(IdeLogLevel level, IdeContext context);

  /**
   * Logs output and error messages on the provided log level.
   *
   * @param outLevel the {@link IdeLogLevel} to use for {@link #getOut()}.
   * @param context the {@link IdeContext} to use.
   * @param errorLevel the {@link IdeLogLevel} to use for {@link #getErr()}.
   */
  void log(IdeLogLevel outLevel, IdeContext context, IdeLogLevel errorLevel);

  /**
   * Throws a {@link CliProcessException} if not {@link #isSuccessful() successful} and otherwise does nothing.
   *
   * @throws CliProcessException if not {@link #isSuccessful() successful}.
   */
  void failOnError() throws CliProcessException;
}
