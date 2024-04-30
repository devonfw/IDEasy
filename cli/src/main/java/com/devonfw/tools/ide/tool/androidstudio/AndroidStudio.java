package com.devonfw.tools.ide.tool.androidstudio;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.version.VersionIdentifier;

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

    if (this.context.getSystemInfo().isWindows()) {
      return STUDIO64_EXE;
    } else if (this.context.getSystemInfo().isLinux()) {
      return STUDIO_BASH;
    } else {
      return "open";
    }
  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    if (this.context.getSystemInfo().isMac()) {
      Path studioPath = getToolPath().resolve("Contents").resolve("MacOS").resolve(STUDIO);
      args = CliArgument.prepend(args, "-na", studioPath.toString(), "--args", this.context.getWorkspacePath().toString());
    } else {
      args = CliArgument.prepend(args, this.context.getWorkspacePath().toString());
    }
    super.runTool(processMode, toolVersion, args);

  }

  @Override
  public boolean install(boolean silent) {

    return super.install(silent);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }
}
