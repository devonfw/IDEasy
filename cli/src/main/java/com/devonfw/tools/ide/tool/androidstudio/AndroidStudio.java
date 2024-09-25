package com.devonfw.tools.ide.tool.androidstudio;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;

/**
 * {@link IdeToolCommandlet} for <a href="https://developer.android.com/studio">AndroidStudio</a>.
 */
public class AndroidStudio extends IdeaBasedIdeToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public AndroidStudio(IdeContext context) {

    super(context, "android-studio", Set.of(Tag.ANDROID_STUDIO));
  }

  @Override
  protected void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("STUDIO_PROPERTIES", this.context.getWorkspacePath().resolve("studio.properties").toString());
  }

  @Override
  protected void postExtract(Path extractedDir) {

    super.postExtract(extractedDir);
    String binaryName;
    if (this.context.getSystemInfo().isWindows()) {
      binaryName = "studio64.exe";
    } else if (this.context.getSystemInfo().isMac()) {
      binaryName = "studio";
    } else {
      binaryName = "studio.sh";
    }
    createStartScript(extractedDir, binaryName);
  }
}
