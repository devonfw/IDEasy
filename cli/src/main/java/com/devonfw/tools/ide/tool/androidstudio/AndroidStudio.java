package com.devonfw.tools.ide.tool.androidstudio;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.io.IOException;
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
  protected String getBinaryName() {

    Path toolBinPath = getToolBinPath();
    if (this.context.getSystemInfo().isWindows()) {
      return STUDIO64_EXE;
    } else if (this.context.getSystemInfo().isLinux()) {
      return STUDIO_BASH;
    } else {
      return STUDIO;
    }
  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    args = CliArgument.prepend(args, this.context.getWorkspacePath().toString());

    install(null, true);
    super.runTool(processMode, toolVersion, args);

  }

  @Override
  public boolean install(EnvironmentContext environmentContext, boolean silent) {

    return super.install(environmentContext, silent);
  }

  @Override
  protected void postInstall() {

    super.postInstall();
    if (this.context.getSystemInfo().isMac()) {
      setMacOsFilePermissions(getToolPath().resolve("Android Studio Preview.app").resolve("Contents").resolve("MacOS").resolve(STUDIO));
    }
  }

  private void setMacOsFilePermissions(Path binaryFile) {

    if (Files.exists(binaryFile)) {
      FileAccess fileAccess = this.context.getFileAccess();
      try {
        fileAccess.makeExecutable(binaryFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }
}
