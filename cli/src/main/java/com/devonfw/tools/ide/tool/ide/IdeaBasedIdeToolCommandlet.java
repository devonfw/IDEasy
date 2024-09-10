package com.devonfw.tools.ide.tool.ide;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.os.MacOsHelper;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.plugin.PluginDescriptor;

public class IdeaBasedIdeToolCommandlet extends IdeToolCommandlet {

  private static final String BUILD_FILE = "build.txt";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public IdeaBasedIdeToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {
    super(context, tool, tags);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {
    String downloadUrl = getDownloadUrl(plugin);

    String pluginId = plugin.getId();

    Path tmpDir = null;

    Step step = this.context.newStep("Install plugin: " + pluginId);
    try {

      Path installationPath = this.getPluginsInstallationPath();
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

  /**
   * @param plugin the {@link PluginDescriptor} to be installer
   * @return a {@link String} representing the download URL.
   */
  private String getDownloadUrl(PluginDescriptor plugin) {
    String downloadUrl = plugin.getUrl();
    String pluginId = plugin.getId();

    String buildVersion = readBuildVersion();

    if (downloadUrl == null || downloadUrl.isEmpty()) {
      downloadUrl = String.format("https://plugins.jetbrains.com/pluginManager?action=download&id=%s&build=%s", pluginId, buildVersion);
    }
    return downloadUrl;
  }

  private String readBuildVersion() {
    Path buildFile = this.getToolPath().resolve(BUILD_FILE);
    if (context.getSystemInfo().isMac()) {
      MacOsHelper macOsHelper = new MacOsHelper(context);
      Path appPath = macOsHelper.findAppDir(macOsHelper.findRootToolPath(this, context));
      buildFile = appPath.resolve("Contents/Resources").resolve(BUILD_FILE);
    }
    try {
      return Files.readString(buildFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + this.getName() + " build version: " + buildFile, e);
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
    String fileName = String.format("%s-plugin-%s%s", this.getName(), pluginId, extension);
    Path downloadedFile = tmpDir.resolve(fileName);
    fileAccess.download(downloadUrl, downloadedFile);
    return downloadedFile;
  }

  private void extractDownloadedPlugin(FileAccess fileAccess, Path downloadedFile, String pluginId) throws IOException {
    Path targetDir = this.getPluginsInstallationPath().resolve(pluginId);
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

  /**
   * Creates a start script for the tool using the tool name.
   *
   * @param extractedDir path to extracted tool directory.
   * @param binaryName name of the binary to add to start script.
   */
  protected void createStartScript(Path extractedDir, String binaryName) {
    Path binFolder = extractedDir.resolve("bin");
    if (!Files.exists(binFolder)) {
      if (this.context.getSystemInfo().isMac()) {
        MacOsHelper macOsHelper = getMacOsHelper();
        Path appDir = macOsHelper.findAppDir(extractedDir);
        binFolder = macOsHelper.findLinkDir(appDir, binaryName);
      } else {
        binFolder = extractedDir;
      }
      assert (Files.exists(binFolder));
    }
    Path bashFile = binFolder.resolve(getName());
    String bashFileContentStart = "#!/usr/bin/env bash\n\"$(dirname \"$0\")/";
    String bashFileContentEnd = "\" $*";
    try {
      Files.writeString(bashFile, bashFileContentStart + binaryName + bashFileContentEnd);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assert (Files.exists(bashFile));
    context.getFileAccess().makeExecutable(bashFile);
  }
}
