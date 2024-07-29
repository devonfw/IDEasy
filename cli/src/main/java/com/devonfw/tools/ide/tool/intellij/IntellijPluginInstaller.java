package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.ide.PluginInstaller;

/**
 * Plugin Installer for {@link Intellij}.
 */
public class IntellijPluginInstaller extends PluginInstaller {

  private static final String BUILD_FILE = "build.txt";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link IdeToolCommandlet}
   */
  public IntellijPluginInstaller(IdeContext context, IdeToolCommandlet commandlet) {
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
      buildFile = context.getSoftwareRepositoryPath().resolve("default").resolve("intellij/intellij").resolve(commandlet.getInstalledVersion().toString())
          .resolve("IntelliJ IDEA" + generateMacEditionString() + ".app").resolve("Contents/Resources").resolve(BUILD_FILE);
    }
    try {
      return Files.readString(buildFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read IntelliJ build version: " + buildFile, e);
    }
  }

  private String generateMacEditionString() {

    String edition = "";
    if (commandlet.getConfiguredEdition().equals("intellij")) {
      edition = " CE";
    }
    return edition;
  }

}
