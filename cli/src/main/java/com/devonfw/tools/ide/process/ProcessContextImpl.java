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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.cli.CliProcessException;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.util.FilenameUtil;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Implementation of {@link ProcessContext}.
 */
public class ProcessContextImpl implements ProcessContext {

  private static final String PREFIX_USR_BIN_ENV = "/usr/bin/env ";
  private static final Predicate<Integer> EXIT_CODE_ACCEPTOR = rc -> rc == ProcessResult.SUCCESS;

  /** The owning {@link IdeContext}. */
  protected final IdeContext context;

  private final ProcessBuilder processBuilder;

  protected final List<String> arguments;

  protected Path executable;

  private String overriddenPath;

  private final List<Path> extraPathEntries;

  private ProcessErrorHandling errorHandling;

  private OutputListener outputListener;

  private Predicate<Integer> exitCodeAcceptor;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public ProcessContextImpl(IdeContext context) {

    super();
    this.context = context;
    this.processBuilder = new ProcessBuilder();
    this.errorHandling = ProcessErrorHandling.THROW_ERR;
    Map<String, String> environment = this.processBuilder.environment();
    for (VariableLine var : this.context.getVariables().collectExportedVariables()) {
      if (var.isExport()) {
        environment.put(var.getName(), var.getValue());
      }
    }
    this.arguments = new ArrayList<>();
    this.extraPathEntries = new ArrayList<>();
    this.exitCodeAcceptor = EXIT_CODE_ACCEPTOR;
  }

  private ProcessContextImpl(ProcessContextImpl parent) {

    super();
    this.context = parent.context;
    this.processBuilder = parent.processBuilder;
    this.errorHandling = ProcessErrorHandling.THROW_ERR;
    this.arguments = new ArrayList<>();
    this.extraPathEntries = parent.extraPathEntries;
    this.exitCodeAcceptor = EXIT_CODE_ACCEPTOR;
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

    this.executable = command;
    return this;
  }

  @Override
  public ProcessContext addArg(String arg) {

    this.arguments.add(arg);
    return this;
  }

  @Override
  public ProcessContext withEnvVar(String key, String value) {

    if (IdeVariables.PATH.getName().equals(key)) {
      this.overriddenPath = value;
    } else {
      this.context.trace("Setting process environment variable {}={}", key, value);
      this.processBuilder.environment().put(key, value);
    }
    return this;
  }

  @Override
  public ProcessContext withPathEntry(Path path) {

    this.extraPathEntries.add(path);
    return this;
  }

  @Override
  public ProcessContext withExitCodeAcceptor(Predicate<Integer> exitCodeAcceptor) {

    this.exitCodeAcceptor = exitCodeAcceptor;
    return this;
  }

  @Override
  public ProcessContext createChild() {

    return new ProcessContextImpl(this);
  }

  @Override
  public void setOutputListener(OutputListener listener) {
    this.outputListener = listener;
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {

    if (this.executable == null) {
      throw new IllegalStateException("Missing executable to run process!");
    }

    SystemPath systemPath = this.context.getPath();
    if ((this.overriddenPath != null) || !this.extraPathEntries.isEmpty()) {
      systemPath = systemPath.withPath(this.overriddenPath, this.extraPathEntries);
    }
    String path = systemPath.toString();
    this.context.trace("Setting PATH for process execution of {} to {}", this.executable.getFileName(), path);
    this.executable = systemPath.findBinary(this.executable);
    this.processBuilder.environment().put(IdeVariables.PATH.getName(), path);
    List<String> args = new ArrayList<>(this.arguments.size() + 4);
    String interpreter = addExecutable(args);
    args.addAll(this.arguments);
    String command = createCommand();
    if (this.context.debug().isEnabled()) {
      String message = createCommandMessage(interpreter, " ...");
      this.context.debug(message);
    }

    try {
      applyRedirects(processMode);
      if (processMode.isBackground()) {
        modifyArgumentsOnBackgroundProcess(processMode);
      }

      this.processBuilder.command(args);

      ConcurrentLinkedQueue<OutputMessage> output = new ConcurrentLinkedQueue<>();

      Process process = this.processBuilder.start();

      try {
        if (Redirect.PIPE == processMode.getRedirectOutput() || Redirect.PIPE == processMode.getRedirectError()) {
          CompletableFuture<Void> outFut = readInputStream(process.getInputStream(), false, output);
          CompletableFuture<Void> errFut = readInputStream(process.getErrorStream(), true, output);
          if (Redirect.PIPE == processMode.getRedirectOutput()) {
            outFut.get();
            if (this.outputListener != null) {
              for (OutputMessage msg : output) {
                this.outputListener.onOutput(msg.message(), msg.error());
              }
            }
          }

          if (Redirect.PIPE == processMode.getRedirectError()) {
            errFut.get();
            if (this.outputListener != null) {
              for (OutputMessage msg : output) {
                this.outputListener.onOutput(msg.message(), msg.error());
              }
            }
          }
        }

        int exitCode;

        if (processMode.isBackground()) {
          exitCode = ProcessResult.SUCCESS;
        } else {
          exitCode = process.waitFor();
        }

        List<OutputMessage> finalOutput = new ArrayList<>(output);
        boolean success = this.exitCodeAcceptor.test(exitCode);
        ProcessResult result = new ProcessResultImpl(this.executable.getFileName().toString(), command, exitCode, success, finalOutput);

        performLogging(result, exitCode, interpreter);

        return result;
      } finally {
        if (!processMode.isBackground()) {
          process.destroy();
        }
      }
    } catch (CliProcessException | IllegalStateException e) {
      // these exceptions are thrown from performLogOnError and we do not want to wrap them (see #593)
      throw e;
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
   * Asynchronously and parallel reads {@link InputStream input stream} and stores it in {@link CompletableFuture}. Inspired by: <a href=
   * "https://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki/57483714#57483714">StackOverflow</a>
   *
   * @param is {@link InputStream}.
   * @param errorStream to identify if the output came from stdout or stderr
   * @return {@link CompletableFuture}.
   */
  private static CompletableFuture<Void> readInputStream(InputStream is, boolean errorStream, ConcurrentLinkedQueue<OutputMessage> outputMessages) {

    return CompletableFuture.supplyAsync(() -> {

      try (InputStreamReader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr)) {

        String line;
        while ((line = br.readLine()) != null) {
          OutputMessage outputMessage = new OutputMessage(errorStream, line);
          outputMessages.add(outputMessage);
        }

        return null;
      } catch (Throwable e) {
        throw new RuntimeException("There was a problem while executing the program", e);
      }
    });
  }

  private String createCommand() {
    String cmd = this.executable.toString();
    StringBuilder sb = new StringBuilder(cmd.length() + this.arguments.size() * 4);
    sb.append(cmd);
    for (String arg : this.arguments) {
      sb.append(' ');
      sb.append(arg);
    }
    return sb.toString();
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

  private String addExecutable(List<String> args) {

    String interpreter = null;
    String fileExtension = FilenameUtil.getExtension(this.executable.getFileName().toString());
    boolean isBashScript = "sh".equals(fileExtension);
    this.context.getFileAccess().makeExecutable(this.executable, true);
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
      args.add(this.context.findBashRequired().toString());
    }
    if ("msi".equalsIgnoreCase(fileExtension)) {
      args.add(0, "/i");
      args.add(0, "msiexec");
    }
    args.add(this.executable.toString());
    return interpreter;
  }

  private void performLogging(ProcessResult result, int exitCode, String interpreter) {

    if (!result.isSuccessful() && (this.errorHandling != ProcessErrorHandling.NONE)) {
      IdeLogLevel ideLogLevel = this.errorHandling.getLogLevel();
      String message = createCommandMessage(interpreter, "\nfailed with exit code " + exitCode + "!");

      context.level(ideLogLevel).log(message);
      result.log(ideLogLevel, context);

      if (this.errorHandling == ProcessErrorHandling.THROW_CLI) {
        throw new CliProcessException(message, result);
      } else if (this.errorHandling == ProcessErrorHandling.THROW_ERR) {
        throw new IllegalStateException(message);
      }
    }
  }

  private void modifyArgumentsOnBackgroundProcess(ProcessMode processMode) {

    assert processMode.isBackground() : "Cannot handle non background process mode!";

    Path bash = this.context.findBash();
    if (bash == null) {
      this.context.warning(
          "Cannot start background process via bash because no bash installation was found. Hence, output will be discarded.");
      this.processBuilder.redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
      return;
    }

    String commandToRunInBackground = buildCommandToRunInBackground();

    this.arguments.clear();
    this.arguments.add(bash.toString());
    this.arguments.add("-c");
    commandToRunInBackground += " & disown";
    this.arguments.add(commandToRunInBackground);

  }

  private void applyRedirects(ProcessMode processMode) {

    Redirect output = processMode.getRedirectOutput();
    Redirect error = processMode.getRedirectError();
    Redirect input = processMode.getRedirectInput();

    if (output != null) {
      this.processBuilder.redirectOutput(output);
    }
    if (error != null) {
      this.processBuilder.redirectError(error);
    }
    if (input != null) {
      this.processBuilder.redirectInput(input);
    }
  }

  private String buildCommandToRunInBackground() {

    if (this.context.getSystemInfo().isWindows()) {

      StringBuilder stringBuilder = new StringBuilder();

      for (String argument : this.arguments) {

        if (SystemInfoImpl.INSTANCE.isWindows() && SystemPath.isValidWindowsPath(argument)) {
          argument = WindowsPathSyntax.MSYS.normalize(argument);
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
