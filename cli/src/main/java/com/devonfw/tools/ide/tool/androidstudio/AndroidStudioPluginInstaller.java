package com.devonfw.tools.ide.tool.androidstudio;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedPluginInstaller;

/**
 * Plugin Installer for {@link AndroidStudio}.
 */
public class AndroidStudioPluginInstaller extends IdeaBasedPluginInstaller {

  /**
   * The constructor
   *
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link IdeToolCommandlet commandlet}.
   */
  public AndroidStudioPluginInstaller(IdeContext context, IdeToolCommandlet commandlet) {

    super(context, commandlet);
  }

  @Override
  public String getMacToolApp() {
    return "Android Studio Preview.app";
  }
}
