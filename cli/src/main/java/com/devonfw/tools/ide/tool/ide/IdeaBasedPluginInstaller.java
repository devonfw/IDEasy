package com.devonfw.tools.ide.tool.ide;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.MacOsHelper;

/**
 * Manager class to install plugins for the {@link IdeToolCommandlet commandlet}.
 */
public class IdeaBasedPluginInstaller extends PluginInstaller {

  private static final String BUILD_FILE = "build.txt";

  /**
   * The constructor
   *
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link IdeToolCommandlet commandlet}.
   */
  public IdeaBasedPluginInstaller(IdeContext context, IdeToolCommandlet commandlet) {

    super(context, commandlet);
  }

  /**
   * @param plugin the {@link PluginDescriptor} to be installer
   * @return a {@link String} representing the download URL.
   */
  public String getDownloadUrl(PluginDescriptor plugin) {
    String downloadUrl = plugin.getUrl();
    String pluginId = plugin.getId();

    String buildVersion = readBuildVersion();

    if (downloadUrl == null || downloadUrl.isEmpty()) {
      downloadUrl = String.format("https://plugins.jetbrains.com/pluginManager?action=download&id=%s&build=%s", pluginId, buildVersion);
    }
    return downloadUrl;
  }

  private String readBuildVersion() {
    Path buildFile = commandlet.getToolPath().resolve(BUILD_FILE);
    if (context.getSystemInfo().isMac()) {
      MacOsHelper macOsHelper = new MacOsHelper(context);
      Path rootToolPath = macOsHelper.findRootToolPath(this.commandlet, context);
      buildFile = rootToolPath.resolve(getMacToolApp()).resolve("Contents/Resources").resolve(BUILD_FILE);
    }
    try {
      return Files.readString(buildFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + commandlet.getName() + " build version: " + buildFile, e);
    }
  }

  public String getMacToolApp() {
    throw new IllegalStateException();
  }
}
