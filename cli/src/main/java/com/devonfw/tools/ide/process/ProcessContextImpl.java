package com.devonfw.tools.ide.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.util.FilenameUtil;

/**
 * Implementation of {@link ProcessContext}.
 */
public class ProcessContextImpl implements ProcessContext {

  private static final String PREFIX_USR_BIN_ENV = "/usr/bin/env ";

  /** The owning {@link IdeContext}. */
  protected final IdeContext context;

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
    this.errorHandling = ProcessErrorHandling.THROW;
    Map<String, String> environment = this.processBuilder.environment();
    for (VariableLine var : this.context.getVariables().collectExportedVariables()) {
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

    if (directory != null) {
      this.processBuilder.directory(directory.toFile());
    } else {
      this.context.debug(
          "Could not set the process builder's working directory! Directory of the current java process is used.");
    }

    return this;
  }

  @Override
  public ProcessContext executable(Path command) {

    if (!this.arguments.isEmpty()) {
      throw new IllegalStateException("Arguments already present - did you forget to call run for previous call?");
    }

    this.executable = this.context.getPath().findBinary(command);
    return this;
  }

  @Override
  public ProcessContext addArg(String arg) {

    this.arguments.add(arg);
    return this;
  }

  @Override
  public ProcessContext withEnvVar(String key, String value) {

    this.processBuilder.environment().put(key, value);
    return this;
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {

    // TODO ProcessMode needs to be configurable for GUI
    if (processMode == ProcessMode.DEFAULT) {
      this.processBuilder.redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
    }

    if (processMode == ProcessMode.DEFAULT_SILENT) {
      this.processBuilder.redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
    }

    if (this.executable == null) {
      throw new IllegalStateException("Missing executable to run process!");
    }
    List<String> args = new ArrayList<>(this.arguments.size() + 4);
    String interpreter = addExecutable(this.executable.toString(), args);
    args.addAll(this.arguments);
    if (this.context.debug().isEnabled()) {
      String message = createCommandMessage(interpreter, " ...");
      this.context.debug(message);
    }

    try {

      if (processMode == ProcessMode.DEFAULT_CAPTURE) {
        this.processBuilder.redirectOutput(Redirect.PIPE).redirectError(Redirect.PIPE);
      } else if (processMode.isBackground()) {
        modifyArgumentsOnBackgroundProcess(processMode);
      }

      this.processBuilder.command(args);

      List<String> out = null;
      List<String> err = null;

      Process process = this.processBuilder.start();

      if (processMode == ProcessMode.DEFAULT_CAPTURE) {
        CompletableFuture<List<String>> outFut = readInputStream(process.getInputStream());
        CompletableFuture<List<String>> errFut = readInputStream(process.getErrorStream());
        out = outFut.get();
        err = errFut.get();
      }

      int exitCode;
      if (processMode.isBackground()) {
        exitCode = ProcessResult.SUCCESS;
      } else {
        exitCode = process.waitFor();
      }

      ProcessResult result = new ProcessResultImpl(exitCode, out, err);
      performLogOnError(result, exitCode, interpreter);

      return result;

    } catch (Exception e) {
      String msg = e.getMessage();
      if ((msg == null) || msg.isEmpty()) {
        msg = e.getClass().getSimpleName();
      }
      throw new IllegalStateException(createCommandMessage(interpreter, " failed: " + msg), e);
    } finally {
      this.arguments.clear();
    }
  }

  /**
   * Asynchronously and parallel reads {@link InputStream input stream} and stores it in {@link CompletableFuture}.
   * Inspired by: <a href=
   * "https://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki/57483714#57483714">StackOverflow</a>
   *
   * @param is {@link InputStream}.
   * @return {@link CompletableFuture}.
   */
  private static CompletableFuture<List<String>> readInputStream(InputStream is) {

    return CompletableFuture.supplyAsync(() -> {

      try (InputStreamReader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr)) {
        return br.lines().toList();
      } catch (Throwable e) {
        throw new RuntimeException("There was a problem while executing the program", e);
      }
    });
  }

  private String createCommandMessage(String interpreter, String suffix) {

    StringBuilder sb = new StringBuilder();
    sb.append("Running command '");
    sb.append(this.executable);
    sb.append("'");
    if (interpreter != null) {
      sb.append(" using ");
      sb.append(interpreter);
    }
    int size = this.arguments.size();
    if (size > 0) {
      sb.append(" with arguments");
      for (int i = 0; i < size; i++) {
        String arg = this.arguments.get(i);
        sb.append(" '");
        sb.append(arg);
        sb.append("'");
      }
    }
    sb.append(suffix);
    return sb.toString();
  }

  private String getSheBang(Path file) {

    try (InputStream in = Files.newInputStream(file)) {
      // "#!/usr/bin/env bash".length() = 19
      byte[] buffer = new byte[32];
      int read = in.read(buffer);
      if ((read > 2) && (buffer[0] == '#') && (buffer[1] == '!')) {
        int start = 2;
        int end = 2;
        while (end < read) {
          byte c = buffer[end];
          if ((c == '\n') || (c == '\r') || (c > 127)) {
            break;
          } else if ((end == start) && (c == ' ')) {
            start++;
          }
          end++;
        }
        String sheBang = new String(buffer, start, end - start, StandardCharsets.US_ASCII).trim();
        if (sheBang.startsWith(PREFIX_USR_BIN_ENV)) {
          sheBang = sheBang.substring(PREFIX_USR_BIN_ENV.length());
        }
        return sheBang;
      }
    } catch (IOException e) {
      // ignore...
    }
    return null;
  }

  private String addExecutable(String exec, List<String> args) {

    String interpreter = null;
    String fileExtension = FilenameUtil.getExtension(exec);
    boolean isBashScript = "sh".equals(fileExtension);
    if (!isBashScript) {
      String sheBang = getSheBang(this.executable);
      if (sheBang != null) {
        String cmd = sheBang;
        int lastSlash = cmd.lastIndexOf('/');
        if (lastSlash >= 0) {
          cmd = cmd.substring(lastSlash + 1);
        }
        if (cmd.equals("bash")) {
          isBashScript = true;
        } else {
          // currently we do not support other interpreters...
        }
      }
    }
    if (isBashScript) {
      interpreter = "bash";
      args.add(this.context.findBash());
    }
    if ("msi".equalsIgnoreCase(fileExtension)) {
      args.add(0, "/i");
      args.add(0, "msiexec");
    }
    args.add(exec);
    return interpreter;
  }

  private void performLogOnError(ProcessResult result, int exitCode, String interpreter) {

    if (!result.isSuccessful() && (this.errorHandling != ProcessErrorHandling.NONE)) {
      String message = createCommandMessage(interpreter, " failed with exit code " + exitCode + "!");
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
        this.context.error("Internal error: Undefined error handling {}", this.errorHandling);
      }
      level.log(message);
    }
  }

  private void modifyArgumentsOnBackgroundProcess(ProcessMode processMode) {

    if (processMode == ProcessMode.BACKGROUND) {
      this.processBuilder.redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
    } else if (processMode == ProcessMode.BACKGROUND_SILENT) {
      this.processBuilder.redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
    } else {
      throw new IllegalStateException("Cannot handle non background process mode!");
    }

    String bash = this.context.findBash();
    if (bash == null) {
      context.warning(
          "Cannot start background process via bash because no bash installation was found. Hence, output will be discarded.");
      this.processBuilder.redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
      return;
    }

    String commandToRunInBackground = buildCommandToRunInBackground();

    this.arguments.clear();
    this.arguments.add(bash);
    this.arguments.add("-c");
    commandToRunInBackground += " & disown";
    this.arguments.add(commandToRunInBackground);

  }

  private String buildCommandToRunInBackground() {

    if (this.context.getSystemInfo().isWindows()) {

      StringBuilder stringBuilder = new StringBuilder();

      for (String argument : this.arguments) {

        if (SystemPath.isValidWindowsPath(argument)) {
          argument = SystemPath.convertWindowsPathToUnixPath(argument);
        }

        stringBuilder.append(argument);
        stringBuilder.append(" ");
      }
      return stringBuilder.toString().trim();
    } else {
      return this.arguments.stream().map(Object::toString).collect(Collectors.joining(" "));
    }
  }
}