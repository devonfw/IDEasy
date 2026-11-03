package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * {@link AbstractCommandlet} to run an AI coding agent (default: Claude Code) in an isolated Docker container with an auto-provisioned
 * IDEasy workspace, without touching the host IDEasy installation.
 * <p>
 * This is a thin wrapper around the {@code docker-sandbox} solution that ships in the settings repository (e.g.
 * {@code $IDE_HOME/settings/docker-sandbox/docker-sandbox.sh} for {@code devonfw/ide-settings}). Every argument after {@code ide docker-sandbox} is
 * forwarded verbatim to that launcher - the commandlet itself understands no options. The launcher's own options (such as {@code --repo},
 * {@code --image}, {@code --agent}, {@code --pull}, {@code --check}, {@code --dry-run}, {@code --ca-cert}, {@code --api-key} and {@code --env}) and the
 * agent arguments following a {@code --} token are documented in the launcher and in the Docker Sandbox documentation.
 */
public final class DockerSandboxCommandlet extends AbstractCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(DockerSandboxCommandlet.class);

  /** The name of this {@link AbstractCommandlet#getName() commandlet}. */
  public static final String NAME = "docker-sandbox";

  /** The name of the folder of the Docker Sandbox solution inside the {@link IdeContext#getSettingsPath() settings} folder. */
  private static final String SANDBOX_FOLDER = "docker-sandbox";

  /** The name of the host launcher script inside the {@link #SANDBOX_FOLDER}. */
  private static final String LAUNCHER = "docker-sandbox.sh";

  /** Exit code reported if the launcher script is missing from the settings folder. */
  private static final int EXIT_CODE_LAUNCHER_NOT_FOUND = 1;

  /** The launcher options that take a value - used to find the position where the forwarded (agent) arguments start. */
  private static final Set<String> LAUNCHER_OPTIONS_WITH_VALUE =
      Set.of("--repo", "--image", "--agent", "--ca-cert", "--api-key", "--env");

  /** The launcher flag options that take no value. */
  private static final Set<String> LAUNCHER_FLAGS = Set.of("--pull", "--check", "--dry-run", "-h", "--help");

  /** All arguments after the keyword, forwarded verbatim to the launcher. */
  private final StringProperty args;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public DockerSandboxCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.args = add(new StringProperty("", false, true, "args"));
  }

  @Override
  public String getName() {

    return NAME;
  }

  @Override
  protected void doRun() {

    Path launcher = this.context.getSettingsPath().resolve(SANDBOX_FOLDER).resolve(LAUNCHER);
    if (!Files.isRegularFile(launcher)) {
      throw new CliException(
          "The Docker Sandbox launcher was not found at " + launcher + ". "
              + "It is provided by your settings repository (e.g. devonfw/ide-settings) in the 'docker-sandbox' folder of the settings folder. "
              + "Please run 'ide update' or verify that your settings repository contains the Docker Sandbox solution.",
          EXIT_CODE_LAUNCHER_NOT_FOUND);
    }

    IdeLogLevel.INTERACTION.log(LOG, "Running the Docker Sandbox launcher: {}", launcher);
    Path bash = this.context.findBashRequired();
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(bash)
        .addArgs(buildLauncherCommand(launcher));
    int exitCode = pc.run(ProcessMode.DEFAULT).getExitCode();
    if (exitCode != ProcessResult.SUCCESS) {
      throw new CliException("The Docker Sandbox launcher exited with status code " + exitCode + ".", exitCode);
    }
  }

  /**
   * Builds the arguments for the launcher process: the path to the launcher script followed by the arguments to forward. Since the IDEasy CLI
   * strips the {@code --} end-of-options token, it is re-inserted before the first argument that is not a known launcher option, so the launcher can
   * still separate its own options from the agent arguments.
   *
   * @param launcher the path to the launcher script.
   * @return the command-line arguments for the process (launcher path first, then the forwarded arguments).
   */
  private List<String> buildLauncherCommand(Path launcher) {

    List<String> result = new ArrayList<>();
    result.add(launcher.toString());
    result.addAll(forwardArgs(this.args.asList()));
    return result;
  }

  /**
   * Re-inserts the {@code --} end-of-options token before the first argument that is not a known launcher option, restoring the boundary the launcher
   * relies on to separate its own options from the agent arguments.
   *
   * @param args the arguments as captured from the CLI.
   * @return the arguments with the {@code --} token re-inserted.
   */
  static List<String> forwardArgs(List<String> args) {

    List<String> result = new ArrayList<>();
    Iterator<String> iterator = args.iterator();
    boolean boundaryInserted = false;
    while (iterator.hasNext()) {
      String arg = iterator.next();
      if (!boundaryInserted) {
        if ("--".equals(arg)) {
          result.add(arg);
          appendRemaining(iterator, result);
          return result;
        }
        if (LAUNCHER_OPTIONS_WITH_VALUE.contains(arg)) {
          result.add(arg);
          if (iterator.hasNext()) {
            result.add(iterator.next()); // the value of the launcher option
          }
          continue;
        }
        if (LAUNCHER_FLAGS.contains(arg)) {
          result.add(arg);
          continue;
        }
        // the first argument that is not a known launcher option marks the start of the agent arguments
        result.add("--");
        boundaryInserted = true;
      }
      result.add(arg);
    }
    return result;
  }

  /**
   * Appends all the remaining elements of the given {@link Iterator} to the given {@link List}.
   *
   * @param iterator the {@link Iterator} to consume.
   * @param result the {@link List} to append the remaining elements to.
   */
  private static void appendRemaining(Iterator<String> iterator, List<String> result) {

    while (iterator.hasNext()) {
      result.add(iterator.next());
    }
  }
}
