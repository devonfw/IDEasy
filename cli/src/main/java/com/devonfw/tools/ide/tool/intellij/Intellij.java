package com.devonfw.tools.ide.tool.intellij;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeToolCommandlet {

  private static final String IDEA = "idea";

  private static final String IDEA64_EXE = IDEA + "64.exe";

  private static final String IDEA_BASH_SCRIPT = IDEA + ".sh";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Intellij(IdeContext context) {

    super(context, "intellij", Set.of(Tag.INTELLIJ));
  }

  @Override
  protected void runIde(String... args) {

    install(true);

    Step stepRun = this.context.newStep("Running IntelliJ");
    try {
      ProcessResult result;
      if (this.context.getSystemInfo().isWindows()) {
        result = runIntelliJ(ProcessMode.BACKGROUND, CliArgument.prepend(args, this.context.getWorkspacePath().toString()));
      } else {
        result = runIntelliJ(ProcessMode.BACKGROUND, CliArgument.prepend(args, "open", "-na", this.context.getWorkspacePath().toString()));
      }
      if (result.isSuccessful()) {
        stepRun.success("Running IntelliJ successfully.");
      } else {
        stepRun.isFailure();
      }
    } catch (Exception e) {
      stepRun.error(e, "Failed to run IntelliJ.");
    } finally {
      stepRun.end();
    }
  }

  /**
   * Runs IntelliJ application.
   *
   * @param processMode - the {@link ProcessMode}.
   * @param args the individual arguments to pass to IntelliJ.
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runIntelliJ(ProcessMode processMode, String... args) {

    Path toolPath;
    if (this.context.getSystemInfo().isWindows()) {
      toolPath = getToolBinPath().resolve(IDEA64_EXE);
    } else {
      toolPath = getToolBinPath().resolve(IDEA_BASH_SCRIPT);
      if (!Files.exists(toolPath)) {
        toolPath = getToolPath().resolve(IDEA_BASH_SCRIPT);
      }
    }

    ProcessContext pc = this.context.newProcess();
    if (processMode == ProcessMode.DEFAULT_CAPTURE) {
      pc.errorHandling(ProcessErrorHandling.ERROR);
    }
    pc.executable(toolPath);
    pc.addArgs(args);

    return pc.run(processMode);
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }
}
