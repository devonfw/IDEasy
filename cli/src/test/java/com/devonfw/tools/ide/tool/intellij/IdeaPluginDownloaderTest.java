package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.ide.IdeaPluginDownloader;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * Test of {@link IdeaPluginDownloader}.
 */
class IdeaPluginDownloaderTest extends AbstractIdeContextTest {

  /**
   * Verifies that a ZIP plugin whose archive root folder differs from its plugin ID is installed under the plugin ID and can therefore be uninstalled.
   *
   * @param tempDir temporary directory used to create the plugin archive.
   * @throws IOException if creating or extracting the test archive fails.
   */
  @Test
  void testZipPluginWithDifferentArchiveRootCanBeUninstalled(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Intellij commandlet = new Intellij(context);
    IdeaPluginDownloader downloader = new IdeaPluginDownloader(context, commandlet);
    FileAccess fileAccess = context.getFileAccess();

    String pluginId = "test.plugin.id";
    String archiveRoot = "different-folder-name";

    ToolPluginDescriptor plugin = new ToolPluginDescriptor(
        pluginId,
        "test-plugin",
        null,
        null,
        true,
        Set.of(),
        Set.of());

    Path pluginArchive = tempDir.resolve("plugin.zip");

    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pluginArchive))) {
      zip.putNextEntry(new ZipEntry(archiveRoot + "/"));
      zip.closeEntry();

      zip.putNextEntry(new ZipEntry(archiveRoot + "/lib/"));
      zip.closeEntry();

      zip.putNextEntry(new ZipEntry(archiveRoot + "/lib/plugin.jar"));
      zip.write(new byte[] { 0 });
      zip.closeEntry();
    }

    Path installationPath = commandlet.getPluginsInstallationPath();
    fileAccess.mkdirs(installationPath);

    // act
    downloader.extractDownloadedPlugin(fileAccess, pluginArchive, pluginId, installationPath);

    // assert
    Path installedPluginPath = installationPath.resolve(pluginId);
    assertThat(installedPluginPath).isDirectory();
    assertThat(installedPluginPath.resolve("lib").resolve("plugin.jar")).exists();
    assertThat(installationPath.resolve(archiveRoot)).doesNotExist();

    // act
    commandlet.uninstallPlugin(plugin);

    // assert
    assertThat(installedPluginPath).doesNotExist();
  }
}
