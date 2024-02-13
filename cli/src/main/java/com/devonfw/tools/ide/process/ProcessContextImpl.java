package com.devonfw.tools.ide.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

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

    this.processBuilder.directory(directory.toFile());
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
  public ProcessResult run(boolean capture, boolean isBackgroundProcess) {

    if (capture && isBackgroundProcess) {
      throw new IllegalStateException(
          "It is not possible for the main process to capture the streams of the subprocess (background process) !");
    }

    if (this.executable == null) {
      throw new IllegalStateException("Missing executable to run process!");
    }
    String executableName = this.executable.toString();
    // pragmatic solution to avoid copying lists/arrays
    this.arguments.add(0, executableName);

    boolean isBashScript = checkAndHandlePossibleBashScript(executableName);

    if (this.context.debug().isEnabled()) {
      String message = createCommandMessage(" ...");
      this.context.debug(message);
    }

    try {

      if (capture) {
        this.processBuilder.redirectOutput(Redirect.PIPE).redirectError(Redirect.PIPE);
      } else if (isBackgroundProcess) {
        modifyArgumentsOnBackgroundProcess(isBashScript);
      }

      this.processBuilder.command(this.arguments);

      Process process = this.processBuilder.start();

      List<String> out = null;
      List<String> err = null;
      if (capture) {
        out = new ArrayList<>();
        err = new ArrayList<>();
        handleCapture(process, out, err);
      }

      int exitCode;
      if (isBackgroundProcess) {
        // TODO: Ask if a background process shall get its own process result or if we should assume success?
        exitCode = ProcessResult.SUCCESS;
      } else {
        exitCode = process.waitFor();
      }

      ProcessResult result = new ProcessResultImpl(exitCode, out, err);
      performLogOnError(result, exitCode);

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

  private boolean hasSheBang(Path file) {

    try (InputStream in = Files.newInputStream(file)) {
      byte[] buffer = new byte[2];
      int read = in.read(buffer);
      if ((read == 2) && (buffer[0] == '#') && (buffer[1] == '!')) {
        return true;
      }
    } catch (IOException e) {
      // ignore...
    }
    return false;
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

  private void handleCapture(Process process, List<String> out, List<String> err) throws IOException {

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

  private boolean checkAndHandlePossibleBashScript(String executableName) {

    String fileExtension = FilenameUtil.getExtension(executableName);
    boolean isBashScript = "sh".equals(fileExtension) || hasSheBang(this.executable);
    if (isBashScript) {
      String bash = "bash";
      if (this.context.getSystemInfo().isWindows()) {
        String findBashOnWindowsResult = findBashOnWindows();
        if (findBashOnWindowsResult != null) {
          bash = findBashOnWindowsResult;
        }
      }
      this.arguments.add(0, bash);
    }
    return isBashScript;
  }

  private void performLogOnError(ProcessResult result, int exitCode) {

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
  }

  private void modifyArgumentsOnBackgroundProcess(boolean isBashScript) {

    if (context.getSystemInfo().isWindows() && !isBashScript) {

      this.context.warning(
          "Currently starting the process as background process in windows will result in lost standard output!");

      this.processBuilder.redirectOutput(Redirect.PIPE).redirectError(Redirect.PIPE);
      List<String> windowsArgs = List.of("cmd.exe", "/c", "start", "cmd.exe", "/k", "start", "/b");
      List<String> newArgs = Stream.concat(windowsArgs.stream(), this.arguments.stream()).toList();
      this.arguments.clear();
      this.arguments.addAll(newArgs);

    } else {
      // just assume unix system for know
      this.arguments.add("&");
    }
  }

}
