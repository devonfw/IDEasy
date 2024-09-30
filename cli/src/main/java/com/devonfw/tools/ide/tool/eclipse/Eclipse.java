package com.devonfw.tools.ide.tool.eclipse;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.eclipse.org/">Eclipse</a>.
 */
public class Eclipse extends IdeToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Eclipse(IdeContext context) {

    super(context, "eclipse", Set.of(Tag.ECLIPSE));
  }

  @Override
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode, ProcessErrorHandling errorHandling) {

    if ((processMode == ProcessMode.DEFAULT_CAPTURE) && this.context.getSystemInfo().isWindows()) {
      pc.executable(Path.of("eclipsec"));
    } else {
      super.configureToolBinary(pc, processMode, errorHandling);
    }
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, ProcessErrorHandling errorHandling, String... args) {

    // configure workspace location
    pc.addArg("-data").addArg(this.context.getWorkspacePath());
    // use keyring from user home to keep secrets and share across projects and workspaces
    pc.addArg("-keyring").addArg(this.context.getUserHome().resolve(".eclipse").resolve(".keyring"));
    // use isolated plugins folder from project instead of modifying eclipse installation in software repo on plugin installation
    pc.addArg("-configuration").addArg(getPluginsInstallationPath().resolve("configuration"));
    if (processMode == ProcessMode.BACKGROUND) {
      // to start eclipse as GUI
      pc.addArg("gui").addArg("-showlocation").addArg(this.context.getIdeHome().getFileName());
    } else if (processMode == ProcessMode.DEFAULT_CAPTURE) {
      pc.addArg("-consoleLog").addArg("-nosplash");
    }
    super.configureToolArgs(pc, processMode, errorHandling, args);
  }

  @Override
  protected void installDependencies() {

    // TODO create eclipse/eclipse/dependencies.json file in ide-urls and delete this method
    getCommandlet(Java.class).install();
  }

  @Override
  public void installPlugin(ToolPluginDescriptor plugin, Step step) {

    ProcessResult result = runTool(ProcessMode.DEFAULT_CAPTURE, null, ProcessErrorHandling.LOG_WARNING, "-application", "org.eclipse.equinox.p2.director",
        "-repository", plugin.url(), "-installIU", plugin.id());
    if (result.isSuccessful()) {
      for (String line : result.getOut()) {
        if (line.contains("Overall install request is satisfiable")) {
          return;
        }
      }
    }
    log(IdeLogLevel.DEBUG, result.getOut());
    log(IdeLogLevel.ERROR, result.getErr());
    step.error("Failed to install plugin {} ({}): exit code was {}", plugin.name(), plugin.id(), result.getExitCode());
  }

  private void log(IdeLogLevel level, List<String> lines) {

    for (String line : lines) {
      if (line.startsWith("!MESSAGE ")) {
        line = line.substring(9);
      }
      this.context.level(level).log(line);
    }
  }

  @Override
  protected void configureWorkspace() {

    Path lockfile = this.context.getWorkspacePath().resolve(".metadata/.lock");
    if (isLocked(lockfile)) {
      throw new CliException("Your workspace is locked at " + lockfile);
    }
    super.configureWorkspace();
  }

  /**
   * @param lockfile the {@link File} pointing to the lockfile to check.
   * @return {@code true} if the given {@link File} is locked, {@code false} otherwise.
   */
  private static boolean isLocked(Path lockfile) {

    if (Files.isRegularFile(lockfile)) {
      try (RandomAccessFile raFile = new RandomAccessFile(lockfile.toFile(), "rw")) {
        FileLock fileLock = raFile.getChannel().tryLock(0, 1, false);
        // success, file was not locked so we immediately unlock again...
        fileLock.release();
        return false;
      } catch (Exception e) {
        return true;
      }
    }
    return false;
  }

}
