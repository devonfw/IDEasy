package com.devonfw.tools.ide.tool.intellij;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Intellij(IdeContext context) {

    super(context, "intellij", Set.of(Tag.INTELLIJ));
  }

  @Override
  public boolean install(boolean silent) {

    for (PluginDescriptor plugin : super.getPluginsMap().values()) {
      if (plugin.isActive()) {
        installPlugin(plugin);
      } else {
        handleInstall4InactivePlugin(plugin);
      }
    }

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  public void postInstall() {

  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

    Path buildFile = this.getToolPath().resolve("build.txt");
    String buildVersion = null;
    String pluginId = plugin.getId();
    String downloadUrl = plugin.getUrl();

    try {
      buildVersion = Files.readString(buildFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read IntelliJ build version: " + buildFile, e);
    }

    if (buildVersion != null && pluginId != null) {
      Path installationPath = this.getPluginsInstallationPath();
      if (!Files.exists(installationPath)) {
        try {
          Files.createDirectories(installationPath);
        } catch (IOException e) {
          throw new IllegalStateException("Failed to create directory " + installationPath, e);
        }
      }

      if (downloadUrl == null || downloadUrl.isEmpty()) {
        downloadUrl = String.format("https://plugins.jetbrains.com/pluginManager?action=download&id=%s&build=%s", pluginId, buildVersion);
      }

      FileAccess fileAccess = this.context.getFileAccess();
      Path tmpDir = fileAccess.createTempDir(pluginId);

      try {
        String extension = getFileExtensionFromUrl(downloadUrl);

        if (extension.isEmpty()) {
          throw new IllegalStateException("Unknown file type for URL: " + downloadUrl);
        }

        String fileName = String.format("intellij-plugin-%s-%s%s", buildVersion, pluginId, extension);
        Path downloadedFile = tmpDir.resolve(fileName);
        fileAccess.download(downloadUrl, downloadedFile);

        Path targetDir = this.getPluginsInstallationPath().resolve(pluginId);

        //        if (".zip".equals(extension)) {
        //          fileAccess.extract(downloadedFile, targetDir);
        //        } else if (".jar".equals(extension)) {
        //          extractJar(downloadedFile, targetDir);
        //        }
        fileAccess.extract(downloadedFile, targetDir);

      } catch (IOException e) {
        throw new IllegalStateException("Failed to download the plugin file: " + downloadUrl, e);
      } finally {
        if (tmpDir != null) {
          fileAccess.delete(tmpDir);
        }
      }
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
    switch (contentType) {
      case "application/zip":
        return ".zip";
      case "application/java-archive":
        return ".jar";
      // Add more content types if needed
      default:
        return "";
    }
  }

}