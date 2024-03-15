package com.devonfw.tools.ide.process;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.util.FilenameUtil;

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
import java.util.stream.Collectors;

/**
 * Implementation of {@link ProcessContext}.
 */
public class ProcessContextImpl implements ProcessContext {

  private static final String PREFIX_USR_BIN_ENV = "/usr/bin/env ";

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
      context.debug(
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

    if (this.executable == null) {
      throw new IllegalStateException("Missing executable to run process!");
    }
    List<String> args = new ArrayList<>(this.arguments.size() + 2);
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

      Process process = this.processBuilder.start();

      List<String> out = null;
      List<String> err = null;

      if (processMode == ProcessMode.DEFAULT_CAPTURE) {
        try (BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
          out = outReader.lines().collect(Collectors.toList());
        }
        try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
          err = errReader.lines().collect(Collectors.toList());
        }
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
    String message = sb.toString();
    return message;
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

  private String findBashOnWindows() {

    // Check if Git Bash exists in the default location
    Path defaultPath = Path.of("C:\\Program Files\\Git\\bin\\bash.exe");
    if (Files.exists(defaultPath)) {
      return defaultPath.toString();
    }

    // If not found in the default location, try the registry query
    String[] bashVariants = { "GitForWindows", "Cygwin\\setup" };
    String[] registryKeys = { "HKEY_LOCAL_MACHINE", "HKEY_CURRENT_USER" };
    String regQueryResult;
    for (String bashVariant : bashVariants) {
      for (String registryKey : registryKeys) {
        String toolValueName = ("GitForWindows".equals(bashVariant)) ? "InstallPath" : "rootdir";
        String command = "reg query " + registryKey + "\\Software\\" + bashVariant + "  /v " + toolValueName + " 2>nul";

        try {
          Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
              output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
              return null;
            }

            regQueryResult = output.toString();
            if (regQueryResult != null) {
              int index = regQueryResult.indexOf("REG_SZ");
              if (index != -1) {
                String path = regQueryResult.substring(index + "REG_SZ".length()).trim();
                return path + "\\bin\\bash.exe";
              }
            }

          }
        } catch (Exception e) {
          return null;
        }
      }
    }
    // no bash found
    throw new IllegalStateException("Could not find Bash. Please install Git for Windows and rerun.");
  }

  private String addExecutable(String executable, List<String> args) {

    if (!SystemInfoImpl.INSTANCE.isWindows()) {
      args.add(executable);
      return null;
    }
    String interpreter = null;
    String fileExtension = FilenameUtil.getExtension(executable);
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
      String bash = "bash";
      interpreter = bash;
      // here we want to have native OS behavior even if OS is mocked during tests...
      // if (this.context.getSystemInfo().isWindows()) {
      if (SystemInfoImpl.INSTANCE.isWindows()) {
        String findBashOnWindowsResult = findBashOnWindows();
        if (findBashOnWindowsResult != null) {
          bash = findBashOnWindowsResult;
        }
      }
      args.add(bash);
    }

    if ("msi".equalsIgnoreCase(fileExtension)) {
        args.add(0, "/i");
        args.add(0, "msiexec");
    }
    args.add(executable);
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
        level.log("Internal error: Undefined error handling {}", this.errorHandling);
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

    String bash = "bash";

    // try to use bash in windows to start the process
    if (context.getSystemInfo().isWindows()) {

      String findBashOnWindowsResult = findBashOnWindows();
      if (findBashOnWindowsResult != null) {

        bash = findBashOnWindowsResult;

      } else {
        context.warning(
            "Cannot start background process in windows! No bash installation found, output will be discarded.");
        this.processBuilder.redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
        return;
      }
    }

    String commandToRunInBackground = buildCommandToRunInBackground();

    this.arguments.clear();
    this.arguments.add(bash);
    this.arguments.add("-c");
    commandToRunInBackground += " & disown";
    this.arguments.add(commandToRunInBackground);

  }

  private String buildCommandToRunInBackground() {

    if (context.getSystemInfo().isWindows()) {

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