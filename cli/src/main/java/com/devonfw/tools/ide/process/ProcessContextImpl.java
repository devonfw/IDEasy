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
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(ProcessContextImpl.class);

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
      LOG.debug(
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
      LOG.trace("Setting process environment variable {}={}", key, value);
      this.processBuilder.environment().put(key, value);
    }
    return this;
  }

  @Override
  public EnvironmentContext removeEnvVar(String key) {

    LOG.trace("Removing process environment variable {}", key);
    this.processBuilder.environment().remove(key);
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
    LOG.trace("Setting PATH for process execution of {} to {}", this.executable.getFileName(), path);
    this.executable = systemPath.findBinary(this.executable);
    if (processMode.launchesNewWindow()) {
      // The command runs in a new terminal window whose working directory is the user's home directory, not the
      // current working directory, so the executable must be an absolute path or it would not be found.
      this.executable = this.executable.toAbsolutePath().normalize();
    }
    this.processBuilder.environment().put(IdeVariables.PATH.getName(), path);
    List<String> args = new ArrayList<>(this.arguments.size() + 4);
    String interpreter = addExecutable(args);
    args.addAll(this.arguments);
    String command = createCommand();
    if (LOG.isDebugEnabled()) {
      String message = createCommandMessage(interpreter, " ...");
      LOG.debug(message);
    }

    try {
      applyRedirects(processMode);
      if (processMode.isBackground()) {
        modifyArgumentsOnBackgroundProcess(processMode, args);
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
          }
          if (Redirect.PIPE == processMode.getRedirectError()) {
            errFut.get();
          }
          if (this.outputListener != null) {
            for (OutputMessage msg : output) {
              this.outputListener.onOutput(msg.message(), msg.error());
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
        ProcessResult result =
            new ProcessResultImpl(this.executable.getFileName().toString(), command, exitCode, success, finalOutput);

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
   * @param outputMessages the queue storing output messages
   * @return {@link CompletableFuture}.
   */
  private static CompletableFuture<Void> readInputStream(InputStream is, boolean errorStream,
      ConcurrentLinkedQueue<OutputMessage> outputMessages) {

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
      args.addFirst("/i");
      args.addFirst("msiexec");
    }
    args.add(this.executable.toString());
    return interpreter;
  }

  private void performLogging(ProcessResult result, int exitCode, String interpreter) {

    if (!result.isSuccessful() && (this.errorHandling != ProcessErrorHandling.NONE)) {
      IdeLogLevel ideLogLevel = this.errorHandling.getLogLevel();
      String message = createCommandMessage(interpreter, "\nfailed with exit code " + exitCode + "!");

      LOG.atLevel(ideLogLevel.getSlf4jLevel()).log(message);
      result.log(ideLogLevel);

      if (this.errorHandling == ProcessErrorHandling.THROW_CLI) {
        throw new CliProcessException(message, result);
      } else if (this.errorHandling == ProcessErrorHandling.THROW_ERR) {
        throw new IllegalStateException(message);
      }
    }
  }

  /**
   * Modifies the argument list to run the command as a background process. On Linux/macOS, uses {@code bash -c} with {@code & disown} for detachment. On
   * Windows, uses {@code cmd.exe /c} with {@code start /b} for detachment or {@code start} for a new window.
   *
   * @param processMode the {@link ProcessMode} determining the background behavior
   * @param args the argument list to modify in place
   */
  private void modifyArgumentsOnBackgroundProcess(ProcessMode processMode, List<String> args) {

    if (!processMode.isBackground()) {
      throw new IllegalStateException(
          "modifyArgumentsOnBackgroundProcess called for a non-background process.");
    }

    if (processMode.launchesNewWindow()) {
      String commandToRunInBackground = buildCommand(args);
      if (this.context.getSystemInfo().isWindows()) {
        modifyArgumentsOnBackgroundProcessForNewWindowWindows(args, commandToRunInBackground);
      } else {
        modifyArgumentsOnBackgroundProcessForNewWindowUnix(args, commandToRunInBackground);
      }
    } else {
      Path bash = this.context.findBash();
      if (bash == null) {
        LOG.warn("Cannot start background process via bash because no bash installation was found. Hence, output will be discarded.");
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
  }

  private String buildCommandToRunInBackground() {

    if (this.context.getSystemInfo().isWindows()) {
      return buildCommandString(arg -> {
        if (SystemInfoImpl.INSTANCE.isWindows() && SystemPath.isValidWindowsPath(arg)) {
          return WindowsPathSyntax.MSYS.normalize(arg);
        }
        return arg;
      });
    }
    return buildCommandString(Function.identity());
  }

  /**
   * Builds a space-separated command string from {@code arguments}, applying the given {@link Function} to each argument for quoting or normalization.
   *
   * @param transformer the function to apply to each argument (e.g., quoting or path normalization)
   * @return the space-separated command string
   */
  private String buildCommandString(Function<String, String> transformer) {

    return this.arguments.stream()
        .map(transformer)
        .reduce((a, b) -> a + " " + b)
        .orElse("");
  }

  /**
   * Modifies arguments for a background process opening in a new window on Linux/macOS using {@code bash -c}.
   *
   * @param args the argument list to modify in place
   * @param command the command string to run in the background
   */
  private void modifyArgumentsOnBackgroundProcessForNewWindowUnix(List<String> args, String command) {

    Path bash = this.context.findBash();
    if (bash == null) {
      LOG.warn("Cannot start background process via bash because no bash installation was found. Hence, output will be discarded.");
      this.processBuilder.redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
      return;
    }

    args.clear();
    args.add(bash.toString());
    args.add("-c");

    String newWindowCommand = buildNewWindowCommand(command);
    args.add(newWindowCommand);

  }

  /**
   * Modifies arguments for a background process opening in a new window on Windows using {@code cmd.exe /c}.
   *
   * @param args the argument list to modify in place
   * @param command the command string to run in the background
   */
  private void modifyArgumentsOnBackgroundProcessForNewWindowWindows(List<String> args, String command) {

    args.clear();
    args.add("cmd.exe");
    args.add("/c");
    args.add("start \"\" cmd.exe /k \"" + command + "\"");

  }

  private String buildCommand(List<String> args) {

    Function<String, String> quoter = this.context.getSystemInfo().isWindows()
        ? this::windowsQuote
        : this::shellQuote;

    return args.stream()
        .map(quoter)
        .reduce((a, b) -> a + " " + b)
        .orElse("");
  }

  /**
   * Build the command for opening a new terminal window on Linux/macOS.
   *
   * @param command the command to run in the new terminal
   * @return the shell command string that opens a new terminal window, or a fallback background command
   */
  private String buildNewWindowCommand(String command) {

    if (this.context.getSystemInfo().isLinux()) {
      return buildLinuxNewWindowCommand(command);
    }

    if (this.context.getSystemInfo().isMac()) {
      return buildMacOsNewWindowCommand(command);
    }

    // Fallback for unsupported platforms
    LOG.warn(
        "No terminal emulator detected for BACKGROUND_NEW_WINDOW on {} - falling back to background execution without new window.",
        this.context.getSystemInfo().getOsName());
    return command + " & disown";
  }

  /**
   * Build the command for opening a new terminal window on Linux. Detects available terminal emulators and uses the correct flag syntax for each.
   *
   * @param command the command to run in the new terminal
   * @return the shell command string that opens a new terminal window, or a fallback background command
   */
  private String buildLinuxNewWindowCommand(String command) {

    String bashCommand = "bash -c " + shellQuote(command + "; exec bash");

    // Prefer explicit terminal emulators over x-terminal-emulator because
    // x-terminal-emulator is only an alternatives symlink and may point to
    // different terminals with different command-line syntax.
    if (isExecutable("gnome-terminal")) {
      return "gnome-terminal -- " + bashCommand + " &";
    }

    if (isExecutable("konsole")) {
      return "konsole -e " + bashCommand + " &";
    }

    if (isExecutable("xfce4-terminal")) {
      return "xfce4-terminal --command=" + shellQuote(bashCommand) + " &";
    }

    if (isExecutable("tilix")) {
      return "tilix -e " + bashCommand + " &";
    }

    if (isExecutable("alacritty")) {
      return "alacritty -e " + bashCommand + " &";
    }

    if (isExecutable("xterm")) {
      return "xterm -e " + bashCommand + " &";
    }

    // Last fallback only. This may still fail depending on what the alternatives
    // symlink points to, but it is better than not trying at all.
    if (isExecutable("x-terminal-emulator")) {
      return "x-terminal-emulator -e " + bashCommand + " &";
    }

    LOG.warn("No terminal emulator found on Linux - falling back to background execution without new window.");

    return "bash -c " + shellQuote(command) + " > /dev/null 2>&1 &";
  }

  private String shellQuote(String value) {

    if (value == null || value.isEmpty()) {
      return "''";
    }
    return "'" + escapeForShellSingleQuote(value) + "'";
  }

  private String windowsQuote(String value) {

    if (value == null || value.isEmpty()) {
      return "\"\"";
    }

    if (value.indexOf('"') >= 0) {
      throw new IllegalArgumentException("Argument must not contain a double quote character on Windows: " + value);
    }

    String escaped = value.replace("^", "^^")
        .replace("&", "^&")
        .replace("|", "^|")
        .replace(">", "^>")
        .replace("<", "^<")
        .replace("%", "%%");

    return "\"" + escaped + "\"";
  }

  /**
   * Build the command for opening a new terminal window on macOS. Prefers iTerm2 via AppleScript, falls back to Terminal.app, then to plain background.
   *
   * @param command the command to run in the new terminal
   * @return the shell command string that opens a new terminal window, or a fallback background command
   */
  private String buildMacOsNewWindowCommand(String command) {

    // Escape for AppleScript string literal: backslashes first, then double quotes
    String escapedForAppleScript = command.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");

    // Check for iTerm2, modern versions ship as /Applications/iTerm.app
    if (isItermInstalled()) {
      String appleScript = "tell application \"iTerm2\"\n"
          + "  activate\n"
          + "  set newWindow to (create window with default profile)\n"
          + "  tell current session of newWindow\n"
          + "    write text \"" + escapedForAppleScript + "; exec bash\"\n"
          + "  end tell\n"
          + "end tell";
      return "osascript -e '" + escapeForShellSingleQuote(appleScript) + "' &";
    }

    // Fallback to Terminal.app
    String terminalScript = "tell application \"Terminal\"\n"
        + "  do script \"" + escapedForAppleScript + "\"\n"
        + "end tell";
    return "osascript -e '" + escapeForShellSingleQuote(terminalScript) + "' &";
  }

  /**
   * Check if iTerm2 is installed on macOS.
   *
   * @return {@code true} if iTerm2 is found
   */
  private boolean isItermInstalled() {

    List<Path> candidates = new ArrayList<>(List.of(
        Path.of("/Applications/iTerm.app"),
        Path.of("/Applications/iTerm2.app")
    ));
    Path homeApps = Path.of(System.getProperty("user.home"), "Applications");
    candidates.add(homeApps.resolve("iTerm.app"));
    candidates.add(homeApps.resolve("iTerm2.app"));
    if (candidates.stream().anyMatch(Files::exists)) {
      return true;
    }
    // Also check if iTerm binary is in PATH, for portable installs, Homebrew, etc.
    return isExecutable("iterm");
  }

  /**
   * Escape a string for embedding inside a shell single-quoted string. Single quotes are replaced with the standard shell sequence: {@code '"'"'}
   *
   * @param s the string to escape
   * @return the escaped string safe for single-quote embedding
   */
  private static String escapeForShellSingleQuote(String s) {

    return s.replace("'", "'\"'\"'");
  }

  /**
   * Check if a command is available in PATH.
   *
   * @param command the command name
   * @return {@code true} if the command is found in PATH
   */
  private boolean isExecutable(String command) {

    SystemPath systemPath = this.context.getPath();
    Path binary = systemPath.findBinary(Path.of(command));

    try {
      return (binary != null) && Files.isExecutable(binary);
    } catch (Exception e) {
      return false;
    }
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
}
