package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;

public class IntellijPluginManager {

  private static final String BUILD_FILE = "build.txt";
  private final IdeContext context;
  private final Intellij commandlet;

  public IntellijPluginManager(IdeContext context, Intellij commandlet) {
    this.context = context;
    this.commandlet = commandlet;
  }

  public void installPlugin(PluginDescriptor plugin) {
    String pluginId = plugin.getId();
    String downloadUrl = plugin.getUrl();

    Path tmpDir = null;
    try {
      String buildVersion = readBuildVersion();

      Path installationPath = commandlet.getPluginsInstallationPath();
      ensureInstallationPathExists(installationPath);

      if (downloadUrl == null || downloadUrl.isEmpty()) {
        downloadUrl = String.format("https://plugins.jetbrains.com/pluginManager?action=download&id=%s&build=%s", pluginId, buildVersion);
      }

      FileAccess fileAccess = context.getFileAccess();
      tmpDir = fileAccess.createTempDir(pluginId);

      Path downloadedFile = downloadPlugin(fileAccess, downloadUrl, tmpDir, buildVersion, pluginId);
      installDownloadedPlugin(fileAccess, downloadedFile, pluginId);

    } catch (IOException e) {
      throw new IllegalStateException("Failed to process plugin installation", e);
    } finally {
      if (tmpDir != null) {
        context.getFileAccess().delete(tmpDir);
      }
    }
  }

  private String readBuildVersion() throws IOException {
    Path buildFile = commandlet.getToolPath().resolve(BUILD_FILE);
    if (context.getSystemInfo().isMac()) {
      buildFile = commandlet.getToolPath().resolve("IntelliJ IDEA" + commandlet.generateMacEditionString() + ".app").resolve("Contents").resolve("Resources")
          .resolve(BUILD_FILE);
    }
    try {
      return Files.readString(buildFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read IntelliJ build version: " + buildFile, e);
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

  private Path downloadPlugin(FileAccess fileAccess, String downloadUrl, Path tmpDir, String buildVersion, String pluginId) throws IOException {
    String extension = getFileExtensionFromUrl(downloadUrl);
    if (extension.isEmpty()) {
      throw new IllegalStateException("Unknown file type for URL: " + downloadUrl);
    }
    String fileName = String.format("intellij-plugin-%s-%s%s", buildVersion, pluginId, extension);
    Path downloadedFile = tmpDir.resolve(fileName);
    fileAccess.download(downloadUrl, downloadedFile);
    return downloadedFile;
  }

  private void installDownloadedPlugin(FileAccess fileAccess, Path downloadedFile, String pluginId) throws IOException {
    Path targetDir = commandlet.getPluginsInstallationPath().resolve(pluginId);
    if (Files.exists(targetDir)) {
      context.info("File already exists");
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
    return getFileExtensionFromContentType(contentType);
  }

  private String getFileExtensionFromContentType(String contentType) {
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
