package com.devonfw.tools.ide.tool.androidstudio;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link IdeToolCommandlet} for <a href="https://developer.android.com/studio">AndroidStudio</a>.
 */
public class AndroidStudio extends IdeaBasedIdeToolCommandlet {

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
      return STUDIO;
    }
  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    args = CliArgument.prepend(args, this.context.getWorkspacePath().toString());

    install(true);
    ProcessContext pc = createProcessContext(Path.of(getBinaryName()), args);
    this.setEnvironment(pc, "STUDIO_PROPERTIES", this.context.getWorkspacePath().resolve("studio.properties"));
    pc.run(processMode);

  }

}
