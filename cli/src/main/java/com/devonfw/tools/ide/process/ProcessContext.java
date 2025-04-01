package com.devonfw.tools.ide.process;

import com.devonfw.tools.ide.log.IdeSubLogger;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper for {@link ProcessBuilder} to simplify its usage and avoid common mistakes and pitfalls.
 */
public interface ProcessContext extends EnvironmentContext {

  /**
   * @param handling the desired {@link ProcessErrorHandling}.
   * @return this {@link ProcessContext} for fluent API calls.
   */
  ProcessContext errorHandling(ProcessErrorHandling handling);

  /**
   * @param directory the {@link Path} to the working directory from where to execute the command.
   * @return this {@link ProcessContext} for fluent API calls.
   */
  ProcessContext directory(Path directory);

  /**
   * Sets the executable command to be {@link #run()}.
   *
   * @param executable the {@link Path} to the command to be executed by {@link #run()}. Depending on your operating system and the extension of the executable
   * or OS specific conventions. So e.g. a *.cmd or *.bat file will be called via CMD shell on windows while a *.sh file will be called via Bash, etc.
   * @return this {@link ProcessContext} for fluent API calls.
   */
  ProcessContext executable(Path executable);

  /**
   * Sets the executable command to be {@link #run()}.
   *
   * @param executable the command to be executed by {@link #run()}.
   * @return this {@link ProcessContext} for fluent API calls.
   * @see #executable(Path)
   */
  default ProcessContext executable(String executable) {

    return executable(Path.of(executable));
  }

  /**
   * Adds a single argument for {@link #executable(Path) command}.
   *
   * @param arg the next argument for {@link #executable(Path) command} to be added.
   * @return this {@link ProcessContext} for fluent API calls.
   */
  ProcessContext addArg(String arg);

  /**
   * Adds a single argument for {@link #executable(Path) command}.
   *
   * @param arg the next argument for {@link #executable(Path) command} to be added.
   * @return this {@link ProcessContext} for fluent API calls.
   */
  default ProcessContext addArg(Object arg) {

    Objects.requireNonNull(arg);
    return addArg(arg.toString());
  }

  /**
   * Adds the given arguments for {@link #executable(Path) command}. E.g. for {@link #executable(Path) command} "mvn" the arguments "clean" and "install" may be
   * added here to run "mvn clean install". If this method would be called again with "-Pmyprofile" and "-X" before {@link #run()} gets called then "mvn clean
   * install -Pmyprofile -X" would be run.
   *
   * @param args the arguments for {@link #executable(Path) command} to be added.
   * @return this {@link ProcessContext} for fluent API calls.
   */
  default ProcessContext addArgs(String... args) {

    for (String arg : args) {
      addArg(arg);
    }
    return this;
  }

  /**
   * Adds the given arguments for {@link #executable(Path) command} as arbitrary Java objects. It will add the {@link Object#toString() string representation}
   * of these arguments to the command.
   *
   * @param args the arguments for {@link #executable(Path) command} to be added.
   * @return this {@link ProcessContext} for fluent API calls.
   */
  default ProcessContext addArgs(Object... args) {

    for (Object arg : args) {
      addArg(arg);
    }
    return this;
  }

  /**
   * Adds the given arguments for {@link #executable(Path) command} as arbitrary Java objects. It will add the {@link Object#toString() string representation}
   * of these arguments to the command.
   *
   * @param args the {@link List} of arguments for {@link #executable(Path) command} to be added.
   * @return this {@link ProcessContext} for fluent API calls.
   */
  default ProcessContext addArgs(List<?> args) {

    for (Object arg : args) {
      addArg(arg);
    }
    return this;
  }

  @Override
  ProcessContext withEnvVar(String key, String value);

  @Override
  ProcessContext withPathEntry(Path path);

  /**
   * Runs the previously configured {@link #executable(Path) command} with the configured {@link #addArgs(String...) arguments}. Will reset the
   * {@link #addArgs(String...) arguments} but not the {@link #executable(Path) command} for sub-sequent calls.
   *
   * @return the exit code. Will be {@link ProcessResult#SUCCESS} on successful completion of the {@link Process}.
   */
  default int run() {

    return run(ProcessMode.DEFAULT).getExitCode();
  }

  /**
   * Runs the given {@code executable} with the given {@code arguments}.
   *
   * @param executable the executable program.
   * @param arguments the program arguments.
   * @throws IllegalStateException if the command failed.
   */
  default void run(String executable, String... arguments) {

    executable(executable).addArgs(arguments).errorHandling(ProcessErrorHandling.THROW_ERR).run();
  }

  /**
   * Runs the given {@code executable} with the given {@code arguments} and returns the expected single line from its
   * {@link ProcessResult#getOut() standard output}.
   *
   * @param executable the executable program.
   * @param arguments the program arguments.
   * @return the single line printed from the command.
   * @throws IllegalStateException if the command failed or did not print a single line as expected.
   */
  default String runAndGetSingleOutput(String executable, String... arguments) {

    return runAndGetSingleOutput(null, executable, arguments);
  }

  /**
   * Runs the given {@code executable} with the given {@code arguments} and returns the expected single line from its
   * {@link ProcessResult#getOut() standard output}.
   *
   * @param logger the {@link IdeSubLogger} used to log errors instead of throwing an exception.
   * @param executable the executable program.
   * @param arguments the program arguments.
   * @return the single line printed from the command.
   * @throws IllegalStateException if the command did not print a single line as expected.
   */
  default String runAndGetSingleOutput(IdeSubLogger logger, String executable, String... arguments) {

    String errorMessage;
    executable(executable).addArgs(arguments);
    if (logger == null) {
      errorHandling(ProcessErrorHandling.THROW_ERR);
    }
    ProcessResult result = run(ProcessMode.DEFAULT_CAPTURE);
    if (result.isSuccessful()) {
      List<String> out = result.getOut();
      int size = out.size();
      if (size == 1) {
        return out.get(0);
      } else if (size == 0) {
        errorMessage = "No output received from " + result.getCommand();
      } else {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected single line of output but received ");
        sb.append(size);
        sb.append(" lines from ");
        sb.append(result.getCommand());
        sb.append(":");
        for (String line : out) {
          sb.append("\n");
          sb.append(line);
        }
        errorMessage = sb.toString();
      }
    } else {
      errorMessage = "Command " + result.getCommand() + " failed with exit code " + result.getExitCode();
    }
    if (logger == null) {
      throw new IllegalStateException(errorMessage);
    } else {
      logger.log(errorMessage);
      return null;
    }
  }

  /**
   * Runs the previously configured {@link #executable(Path) command} with the configured {@link #addArgs(String...) arguments}. Will reset the
   * {@link #addArgs(String...) arguments} but not the {@link #executable(Path) command} for sub-sequent calls.
   *
   * @param processMode {@link ProcessMode}
   * @return the {@link ProcessResult}.
   */
  ProcessResult run(ProcessMode processMode);

}
