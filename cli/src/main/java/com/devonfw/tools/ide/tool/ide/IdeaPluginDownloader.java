package com.devonfw.tools.ide.tool.ide;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.os.MacOsHelper;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * Used for a direct download and installation of idea plugins
 */
public class IdeaPluginDownloader {

  private static final String BUILD_FILE = "build.txt";
  private final IdeContext context;
  private final IdeaBasedIdeToolCommandlet commandlet;

  /**
   * the constructor
   *
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link IdeaBasedIdeToolCommandlet} to use.
   */
  public IdeaPluginDownloader(IdeContext context, IdeaBasedIdeToolCommandlet commandlet) {
    this.context = context;
    this.commandlet = commandlet;
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   * @param pc the {@link ProcessContext} to use.
   * @return boolean {@code true} if successful installed, {@code false} if not.
   */
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {
    String downloadUrl = getDownloadUrl(plugin);

    String pluginId = plugin.id();

    Path tmpDir = null;

    try {
      Path installationPath = this.commandlet.getPluginsInstallationPath();
      ensureInstallationPathExists(installationPath);

      FileAccess fileAccess = context.getFileAccess();
      tmpDir = fileAccess.createTempDir(pluginId);

      Path downloadedFile = downloadPlugin(fileAccess, downloadUrl, tmpDir, pluginId);
      extractDownloadedPlugin(fileAccess, downloadedFile, pluginId);

      step.success();
      return true;
    } catch (IOException e) {
      step.error(e);
      throw new IllegalStateException("Failed to process installation of plugin: " + pluginId, e);
    } finally {
      if (tmpDir != null) {
        context.getFileAccess().delete(tmpDir);
      }
    }
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor} to be installer
   * @return a {@link String} representing the download URL.
   */
  private String getDownloadUrl(ToolPluginDescriptor plugin) {
    String downloadUrl = plugin.url();
    String pluginId = URLEncoder.encode(plugin.id(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");

    String buildVersion = readBuildVersion();

    if (downloadUrl == null || downloadUrl.isEmpty()) {
      downloadUrl = String.format("https://plugins.jetbrains.com/pluginManager?action=download&id=%s&build=%s", pluginId, buildVersion);
    }
    return downloadUrl;
  }

  private String readBuildVersion() {
    Path buildFile = this.commandlet.getToolPath().resolve(BUILD_FILE);
    if (context.getSystemInfo().isMac()) {
      MacOsHelper macOsHelper = new MacOsHelper(context);
      Path appPath = macOsHelper.findAppDir(macOsHelper.findRootToolPath(this.commandlet, context));
      buildFile = appPath.resolve("Contents/Resources").resolve(BUILD_FILE);
    }
    try {
      return Files.readString(buildFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + this.commandlet.getName() + " build version: " + buildFile, e);
    }
  }

  private void ensureInstallationPathExists(Path installationPath) throws IOException {
    if (!Files.exists(installationPath)) {
      try {
        Files.createDirectories(installationPath);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to create directory " + installationPath, e);
      }
    }
  }

  private Path downloadPlugin(FileAccess fileAccess, String downloadUrl, Path tmpDir, String pluginId) throws IOException {
    String extension = getFileExtensionFromUrl(downloadUrl);
    if (extension.isEmpty()) {
      throw new IllegalStateException("Unknown file type for URL: " + downloadUrl);
    }
    String fileName = String.format("%s-plugin-%s%s", this.commandlet.getName(), pluginId, extension);
    Path downloadedFile = tmpDir.resolve(fileName);
    fileAccess.download(downloadUrl, downloadedFile);
    return downloadedFile;
  }

  private void extractDownloadedPlugin(FileAccess fileAccess, Path downloadedFile, String pluginId) throws IOException {
    Path targetDir = this.commandlet.getPluginsInstallationPath().resolve(pluginId);
    if (Files.exists(targetDir)) {
      context.info("Plugin already installed, target directory already existing: {}", targetDir);
    } else {
      fileAccess.extract(downloadedFile, targetDir);
    }
  }

  private String getFileExtensionFromUrl(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("HEAD");
    connection.connect();

    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new IOException("Failed to fetch file headers: HTTP " + responseCode);
    }

    String contentType = connection.getContentType();
    if (contentType == null) {
      return "";
    }
    return switch (contentType) {
      case "application/zip" -> ".zip";
      case "application/java-archive" -> ".jar";
      default -> "";
    };
  }
}
