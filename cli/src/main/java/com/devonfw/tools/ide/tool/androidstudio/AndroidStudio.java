package com.devonfw.tools.ide.tool.androidstudio;

import java.nio.file.Files;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaPluginDownloader;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * {@link IdeToolCommandlet} for <a href="https://developer.android.com/studio">AndroidStudio</a>.
 */
public class AndroidStudio extends IdeaBasedIdeToolCommandlet {

  private static final String STUDIO = "studio";

  private static final String STUDIO64_EXE = STUDIO + "64.exe";

  private static final String STUDIO_BASH_SCRIPT = STUDIO + ".sh";

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
    } else {
      if (Files.exists(this.getToolBinPath().resolve(STUDIO))) {
        return STUDIO;
      } else if (Files.exists(this.getToolBinPath().resolve(STUDIO_BASH_SCRIPT))) {
        return STUDIO_BASH_SCRIPT;
      } else {
        return STUDIO;
      }
    }
  }

  @Override
  protected String getIdeProductPrefix() {

    return STUDIO;
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    // point at the folder being opened (getOpenPath()) so the shell env and the launch agree: the opened project's own studio.properties when a
    // --project folder was given, the managed workspace's otherwise (default behavior unchanged)
    environmentContext.withEnvVar("STUDIO_PROPERTIES", getOpenPath().resolve("studio.properties").toString());
  }

  @Override
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {
    IdeaPluginDownloader ideaPluginDownloader = new IdeaPluginDownloader(this.context, this);
    return ideaPluginDownloader.installPlugin(plugin, step, pc);
  }
}
