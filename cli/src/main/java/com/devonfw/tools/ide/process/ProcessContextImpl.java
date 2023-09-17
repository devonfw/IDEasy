package com.devonfw.tools.ide.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.util.FilenameUtil;

/**
 * Implementation of {@link ProcessContext}.
 */
public final class ProcessContextImpl implements ProcessContext {

  private final IdeContext context;

  private final ProcessBuilder processBuilder;

  private final List<String> arguments;

  private Path executable;

  private ProcessErrorHandling errorHandling;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public ProcessContextImpl(IdeContext context) {

    super();
    this.context = context;
    this.processBuilder = new ProcessBuilder();
    // TODO needs to be configurable for GUI
    this.processBuilder.redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
    this.errorHandling = ProcessErrorHandling.THROW;
    Map<String, String> environment = this.processBuilder.environment();
    for (VariableLine var : this.context.getVariables().collectVariables()) {
      if (var.isExport()) {
        environment.put(var.getName(), var.getValue());
      }
    }
    this.arguments = new ArrayList<>();
  }

  @Override
  public ProcessContext errorHandling(ProcessErrorHandling handling) {

    Objects.requireNonNull(handling);
    this.errorHandling = handling;
    return this;
  }

  @Override
  public ProcessContext directory(Path directory) {

    this.processBuilder.directory(directory.toFile());
    return this;
  }

  @Override
  public ProcessContext executable(Path command) {

    if (!this.arguments.isEmpty()) {
      throw new IllegalStateException("Arguments already present - did you forget to call run for previous call?");
    }
    Path exe = command;
    if (exe.isAbsolute()) {
      Path parent = command.getParent();
      String filename = command.getFileName().toString();
      String extension = FilenameUtil.getExtension(filename);
      if (extension == null) {
        if (this.context.getSystemInfo().isWindows()) {
          Path cmd = parent.resolve(filename + ".cmd");
          if (Files.exists(cmd)) {
            exe = cmd;
          } else {
            cmd = parent.resolve(filename + ".bat");
            if (Files.exists(cmd)) {
              exe = cmd;
            }
          }
        }
      }
    }
    if (!exe.equals(command)) {
      this.context.debug("Using " + exe.getFileName() + " for " + command);
    }
    this.executable = exe;
    return this;
  }

  @Override
  public ProcessContext addArg(String arg) {

    this.arguments.add(arg);
    return this;
  }

  @Override
  public ProcessResult run(boolean capture) {

    if (this.executable == null) {
      throw new IllegalStateException("Missing executable to run process!");
    }
    // pragmatic solution to avoid copying lists/arrays
    this.arguments.add(0, this.executable.toString());
    this.processBuilder.command(this.arguments);
    if (this.context.debug().isEnabled()) {
      String message = createCommandMessage(" ...");
      this.context.debug(message);
    }
    try {
      if (capture) {
        this.processBuilder.redirectOutput(Redirect.PIPE).redirectError(Redirect.PIPE);
      }
      List<String> out = null;
      List<String> err = null;
      Process process = this.processBuilder.start();
      if (capture) {
        out = new ArrayList<>();
        err = new ArrayList<>();
        try (BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
          String outLine = "";
          String errLine = "";
          while ((outLine != null) || (errLine != null)) {
            if (outLine != null) {
              outLine = outReader.readLine();
              if (outLine != null) {
                out.add(outLine);
              }
            }
            if (errLine != null) {
              errLine = errReader.readLine();
              if (errLine != null) {
                err.add(errLine);
              }
            }
          }
        }
      }
      int exitCode = process.waitFor();
      ProcessResult result = new ProcessResultImpl(exitCode, out, err);
      if (!result.isSuccessful() && (this.errorHandling != ProcessErrorHandling.NONE)) {
        String message = createCommandMessage(" failed with exit code " + exitCode + "!");
        if (this.errorHandling == ProcessErrorHandling.THROW) {
          throw new CliException(message, exitCode);
        }
        IdeSubLogger level;
        if (this.errorHandling == ProcessErrorHandling.ERROR) {
          level = this.context.error();
        } else if (this.errorHandling == ProcessErrorHandling.WARNING) {
          level = this.context.warning();
        } else {
          level = this.context.error();
          level.log("Internal error: Undefined error handling {}", this.errorHandling);
        }
        level.log(message);
      }
      return result;
    } catch (Exception e) {
      String msg = e.getMessage();
      if ((msg == null) || msg.isEmpty()) {
        msg = e.getClass().getSimpleName();
      }
      throw new IllegalStateException(createCommandMessage(" failed: " + msg), e);
    } finally {
      this.arguments.clear();
    }
  }

  private String createCommandMessage(String suffix) {

    StringBuilder sb = new StringBuilder();
    sb.append("Running command '");
    sb.append(this.executable);
    sb.append("'");
    int size = this.arguments.size();
    if (size > 1) {
      sb.append(" with arguments");
      for (int i = 1; i < size; i++) {
        String arg = this.arguments.get(i);
        sb.append(" '");
        sb.append(arg);
        sb.append("'");
      }
    }
    sb.append(suffix);
    String message = sb.toString();
    return message;
  }

}
