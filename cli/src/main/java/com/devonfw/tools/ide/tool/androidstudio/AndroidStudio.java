package com.devonfw.tools.ide.tool.androidstudio;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
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
 * {@link IdeToolCommandlet} for <a href="https://developer.android.com/studio">AndroidStudio</a>.
 */
public class AndroidStudio extends IdeToolCommandlet {

  private static final String STUDIO = "studio";

  private static final String STUDIO64_EXE = STUDIO + "64.exe";

  private static final String STUDIO_BASH = STUDIO + ".sh";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public AndroidStudio(IdeContext context) {

    super(context, "android-studio", Set.of(Tag.ANDROID_STUDIO));
  }

  @Override
  protected void runIde(String... args) {

    install(true);

    Step stepRun = this.context.newStep("Running Android Studio");
    try {
      ProcessResult result;
      if (this.context.getSystemInfo().isWindows()) {
        result = runAndroidStudio(ProcessMode.BACKGROUND, CliArgument.prepend(args, this.context.getWorkspacePath().toString()));
      } else {
        result = runAndroidStudio(ProcessMode.BACKGROUND, CliArgument.prepend(args, "open", "-na", this.context.getWorkspacePath().toString()));
      }
      if (result.isSuccessful()) {
        stepRun.success("Running Android Studio successfully.");
      } else {
        stepRun.isFailure();
      }

    } catch (Exception e) {
      stepRun.error(e, "Failed to run Android Studio.");
    } finally {
      stepRun.end();
    }

  }

  /**
   * Runs AndroidStudio application.
   *
   * @param processMode - the {@link ProcessMode}.
   * @param args the individual arguments to pass to Android Studio.
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runAndroidStudio(ProcessMode processMode, String... args) {

    Path toolPath;
    // TODO: Check if this can be optimized.
    if (this.context.getSystemInfo().isWindows()) {
      toolPath = getToolBinPath().resolve(STUDIO64_EXE);
    } else {
      // check bin folder
      toolPath = getToolBinPath().resolve(STUDIO);
      if (!Files.exists(toolPath)) {
        // check tool root folder
        toolPath = getToolPath().resolve(STUDIO);
      }
    }
    ProcessContext pc = this.context.newProcess();

    if (Files.exists(toolPath)) {
      pc.executable(toolPath);
    }

    if (processMode == ProcessMode.DEFAULT_CAPTURE) {
      pc.errorHandling(ProcessErrorHandling.ERROR);
    }
    pc.addArgs(args);

    return pc.run(processMode);
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  protected void postInstall() {

    super.postInstall();
    Path scriptPath = getToolBinPath().resolve(STUDIO_BASH);

    // TODO: Check if this is still needed.
    if (Files.exists(scriptPath)) {
      FileAccess fileAccess = this.context.getFileAccess();
      fileAccess.symlink(scriptPath, getToolBinPath().resolve(STUDIO), true);
    }
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }
}
