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
import java.util.Optional;

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
 * Used for a direct download and installation of idea plugins.
 */
public class IdeaPluginDownloader {

  private static final Logger LOG = LoggerFactory.getLogger(IdeaPluginDownloader.class);

  private static final String BUILD_FILE = "build.txt";

  private final IdeContext context;

  private final IdeaBasedIdeToolCommandlet commandlet;

  /**
   * The constructor.
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
   * @return boolean {@code true} if successfully installed, {@code false} otherwise.
   */
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {

    String downloadUrl = getDownloadUrl(plugin);
    String pluginId = plugin.id();

    Path tmpDir = null;

    try {
      Path installationPath = this.commandlet.getPluginsInstallationPath();
      ensureInstallationPathExists(installationPath);

      FileAccess fileAccess = this.context.getFileAccess();
      tmpDir = fileAccess.createTempDir(pluginId);

      Path downloadedFile = downloadPlugin(fileAccess, downloadUrl, tmpDir, pluginId);
      extractDownloadedPlugin(fileAccess, downloadedFile, pluginId, installationPath);

      step.success();
      return true;

    } catch (IOException e) {

      step.error(e);
      throw new IllegalStateException("Failed to process installation of plugin: " + pluginId, e);

    } finally {

      if (tmpDir != null) {
        this.context.getFileAccess().delete(tmpDir);
      }
    }
  }

  /**
   * @param plugin the {@link ToolPluginDescriptor} to be installed.
   * @return a {@link String} representing the download URL.
   */
  private String getDownloadUrl(ToolPluginDescriptor plugin) {

    String downloadUrl = plugin.url();

    String pluginId = URLEncoder.encode(plugin.id(), StandardCharsets.UTF_8)
        .replaceAll("\\+", "%20");

    String buildVersion = readBuildVersion();

    if ((downloadUrl == null) || downloadUrl.isEmpty()) {
      downloadUrl = String.format(
          "https://plugins.jetbrains.com/pluginManager?action=download&id=%s&build=%s",
          pluginId,
          buildVersion);
    }

    return downloadUrl;
  }

  private String readBuildVersion() {

    Path buildFile = this.commandlet.getToolPath().resolve(BUILD_FILE);

    if (this.context.getSystemInfo().isMac()) {
      MacOsHelper macOsHelper = new MacOsHelper(this.context);
      Path appPath = macOsHelper.findAppDir(
          macOsHelper.findRootToolPath(this.commandlet, this.context));

      buildFile = appPath.resolve("Contents/Resources").resolve(BUILD_FILE);
    }

    try {
      return Files.readString(buildFile);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to read " + this.commandlet.getName() + " build version: " + buildFile,
          e);
    }
  }

  private void ensureInstallationPathExists(Path installationPath) throws IOException {

    if (!Files.exists(installationPath)) {
      try {
        Files.createDirectories(installationPath);
      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed to create directory " + installationPath,
            e);
      }
    }
  }

  private Path downloadPlugin(FileAccess fileAccess, String downloadUrl, Path tmpDir,
      String pluginId) throws IOException {

    String extension = getFileExtensionFromUrl(downloadUrl);

    if (extension.isEmpty()) {
      throw new IllegalStateException("Unknown file type for URL: " + downloadUrl);
    }

    String fileName = String.format(
        "%s-plugin-%s%s",
        this.commandlet.getName(),
        pluginId,
        extension);

    Path downloadedFile = tmpDir.resolve(fileName);

    fileAccess.download(downloadUrl, downloadedFile);

    return downloadedFile;
  }

  public void extractDownloadedPlugin(FileAccess fileAccess, Path downloadedFile,
      String pluginId, Path installationPath) throws IOException {

    String fileName = downloadedFile.getFileName().toString();
    Path targetDir = installationPath.resolve(pluginId);

    if (Files.exists(targetDir)) {
      LOG.info("Plugin already installed, target directory already existing: {}", targetDir);
      return;
    }

    if (fileName.endsWith(".zip")) {

      Path extractionDir = downloadedFile.getParent().resolve(pluginId + "-extracted");
      fileAccess.mkdirs(extractionDir);

      try {
        fileAccess.extractZip(downloadedFile, extractionDir);

        List<Path> extractedDirectories = fileAccess.listChildren(extractionDir, Files::isDirectory);

        if (extractedDirectories.size() != 1) {
          throw new IllegalStateException(
              "Expected exactly one plugin root directory in archive " + downloadedFile);
        }

        Path pluginRoot = extractedDirectories.getFirst();
        Files.move(pluginRoot, targetDir);

      } finally {
        fileAccess.delete(extractionDir);
      }

    } else if (fileName.endsWith(".jar")) {

      fileAccess.extractJar(downloadedFile, targetDir);

    } else {
      throw new IllegalStateException(
          "Unsupported plugin archive: " + downloadedFile);
    }
  }

  private String getFileExtensionFromUrl(String urlString) throws RuntimeException {

    URI uri = null;

    try (HttpClient client = HttpClientFactory.create()) {

      uri = URI.create(urlString);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(uri)
          .method("HEAD", HttpRequest.BodyPublishers.noBody())
          .timeout(Duration.ofSeconds(5))
          .build();

      HttpResponse<?> response = client.send(
          request,
          HttpResponse.BodyHandlers.ofString());

      int responseCode = response.statusCode();

      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new RuntimeException(
            "Failed to fetch file headers: HTTP " + responseCode);
      }

      Optional<String> contentType = response.headers().firstValue("content-type");

      if (contentType.isPresent()) {
        String type = contentType.get().toLowerCase();

        if (type.startsWith("application/zip")) {
          return ".zip";
        }

        if (type.startsWith("application/java-archive")) {
          return ".jar";
        }
      }

      Optional<String> contentDisposition = response.headers().firstValue("content-disposition");

      if (contentDisposition.isPresent()) {
        String disposition = contentDisposition.get().toLowerCase();

        if (disposition.contains(".zip")) {
          return ".zip";
        }

        if (disposition.contains(".jar")) {
          return ".jar";
        }
      }

      String path = uri.getPath().toLowerCase();

      if (path.endsWith(".zip")) {
        return ".zip";
      }

      if (path.endsWith(".jar")) {
        return ".jar";
      }

      return "";

    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to perform HEAD request of URL " + uri,
          e);
    }
  }
}
