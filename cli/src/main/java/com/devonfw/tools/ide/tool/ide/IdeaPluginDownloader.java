package com.devonfw.tools.ide.tool.ide;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.HttpClientFactory;
import com.devonfw.tools.ide.os.MacOsHelper;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * Used for a direct download and installation of idea plugins
 */
public class IdeaPluginDownloader {

  private static final Logger LOG = LoggerFactory.getLogger(IdeaPluginDownloader.class);

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

  void extractDownloadedPlugin(FileAccess fileAccess, Path downloadedFile, String pluginId) throws IOException {
    Path targetDir = this.commandlet.getPluginsInstallationPath().resolve(pluginId);
    if (Files.exists(targetDir)) {
      LOG.info("Plugin already installed, target directory already existing: {}", targetDir);
    } else if (downloadedFile.getFileName().toString().endsWith(".zip")) {
      // a plugin ZIP contains a single plugin folder (e.g. «name»/lib/*.jar) that has to be installed as is and not flattened like FileAccess.extract does.
      // It is installed as «pluginId» so it can be found by uninstallPlugin.
      Path extractedDir = fileAccess.createTempDir("extract-" + pluginId);
      try {
        fileAccess.extractZip(downloadedFile, extractedDir);
        List<Path> pluginDirs = fileAccess.listChildren(extractedDir, Files::isDirectory);
        if (pluginDirs.size() != 1) {
          throw new IllegalStateException("Expected exactly one plugin folder in " + downloadedFile + " but found " + pluginDirs);
        }
        fileAccess.move(pluginDirs.getFirst(), targetDir);
      } finally {
        fileAccess.delete(extractedDir);
      }
    } else {
      fileAccess.extract(downloadedFile, targetDir);
    }
  }

  private String getFileExtensionFromUrl(String urlString) throws RuntimeException {

    URI uri = null;
    HttpRequest request;
    try (HttpClient client = HttpClientFactory.create()) {
      uri = URI.create(urlString);
      request = HttpRequest.newBuilder().uri(uri)
          .method("HEAD", HttpRequest.BodyPublishers.noBody()).timeout(Duration.ofSeconds(5)).build();

      HttpResponse<?> res = client.send(request, HttpResponse.BodyHandlers.ofString());

      int responseCode = res.statusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new RuntimeException("Failed to fetch file headers: HTTP " + responseCode);
      }

      // content-type may have parameters (e.g. "application/zip;charset=utf-8")
      String contentType = res.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
      if (contentType.startsWith("application/zip")) {
        return ".zip";
      } else if (contentType.startsWith("application/java-archive")) {
        return ".jar";
      }
      // fall back to the filename for unspecific content-types (e.g. "application/octet-stream")
      String fileName = res.headers().firstValue("content-disposition").orElse(res.uri().getPath()).toLowerCase(Locale.ROOT);
      if (fileName.contains(".zip")) {
        return ".zip";
      } else if (fileName.contains(".jar")) {
        return ".jar";
      }
      return "";
    } catch (Exception e) {
      throw new RuntimeException("Failed to perform HEAD request of URL " + uri, e);
    }
  }
}
