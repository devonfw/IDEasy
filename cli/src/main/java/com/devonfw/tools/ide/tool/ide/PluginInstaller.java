package com.devonfw.tools.ide.tool.ide;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.step.Step;

/**
 * Manager class to install plugins for the {@link IdeToolCommandlet commandlet}.
 */
public class PluginInstaller {

  protected final IdeContext context;
  protected final IdeToolCommandlet commandlet;

  /**
   * The constructor
   *
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link IdeToolCommandlet commandlet}.
   */
  public PluginInstaller(IdeContext context, IdeToolCommandlet commandlet) {
    this.context = context;
    this.commandlet = commandlet;
  }

  /**
   * Installs a plugin.
   *
   * @param plugin the {@link PluginDescriptor}.
   * @param downloadUrl the download URL for the plugin.
   */
  public void installPlugin(PluginDescriptor plugin, String downloadUrl) {
    String pluginId = plugin.getId();

    Path tmpDir = null;

    Step step = this.context.newStep("Install plugin: " + pluginId);
    try {

      Path installationPath = commandlet.getPluginsInstallationPath();
      ensureInstallationPathExists(installationPath);

      FileAccess fileAccess = context.getFileAccess();
      tmpDir = fileAccess.createTempDir(pluginId);

      Path downloadedFile = downloadPlugin(fileAccess, downloadUrl, tmpDir, pluginId);
      extractDownloadedPlugin(fileAccess, downloadedFile, pluginId);

      step.success();

    } catch (IOException e) {
      step.error(e);
      throw new IllegalStateException("Failed to process installation of plugin: " + pluginId, e);
    } finally {
      if (tmpDir != null) {
        context.getFileAccess().delete(tmpDir);
      }
      step.close();
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
    String fileName = String.format("%s-plugin-%s%s", commandlet.getName(), pluginId, extension);
    Path downloadedFile = tmpDir.resolve(fileName);
    fileAccess.download(downloadUrl, downloadedFile, false);
    return downloadedFile;
  }

  private void extractDownloadedPlugin(FileAccess fileAccess, Path downloadedFile, String pluginId) throws IOException {
    Path targetDir = commandlet.getPluginsInstallationPath().resolve(pluginId);
    if (Files.exists(targetDir)) {
      context.info("Plugin already installed, target directory already existing: ", targetDir);
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
