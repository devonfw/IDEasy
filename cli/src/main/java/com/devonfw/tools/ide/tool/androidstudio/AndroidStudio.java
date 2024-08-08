package com.devonfw.tools.ide.tool.androidstudio;

import java.util.Set;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedPluginInstaller;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.version.VersionIdentifier;

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
      return STUDIO;
    }
  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    args = CliArgument.prepend(args, this.context.getWorkspacePath().toString());

    install(true);
    super.runTool(processMode, toolVersion, args);

  }

  @Override
  protected void postInstall() {

    super.postInstall();
    EnvironmentVariables envVars = this.context.getVariables().getByType(EnvironmentVariablesType.CONF);
    envVars.set("STUDIO_PROPERTIES", this.context.getWorkspacePath().resolve("studio.properties").toString(), true);
    envVars.save();
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

    IdeaBasedPluginInstaller pluginInstaller = new IdeaBasedPluginInstaller(context, this);
    String downloadUrl = pluginInstaller.getDownloadUrl(plugin);
    pluginInstaller.installPlugin(plugin, downloadUrl);
  }

  @Override
  public String getMacToolApp() {
    return "Android Studio Preview.app";
  }
}
