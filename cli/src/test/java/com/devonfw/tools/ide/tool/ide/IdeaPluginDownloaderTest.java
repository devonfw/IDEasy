package com.devonfw.tools.ide.tool.ide;

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
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * Test of {@link IdeaPluginDownloader}.
 */
class IdeaPluginDownloaderTest extends AbstractIdeContextTest {

  /**
   * Tests that a ZIP plugin keeps its folder structure but is installed in a folder named by its plugin ID instead of its archive root folder so that it can be
   * uninstalled.
   *
   * @param tempDir the temporary directory to create the plugin archive in.
   * @throws IOException on error.
   */
  @Test
  void testZipPluginIsInstalledByIdAndCanBeUninstalled(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Intellij intellij = new Intellij(context);
    IdeaPluginDownloader downloader = new IdeaPluginDownloader(context, intellij);
    ToolPluginDescriptor plugin = new ToolPluginDescriptor("test.plugin.id", "test-plugin", null, null, true, Set.of(), Set.of());
    Path pluginArchive = tempDir.resolve("plugin.zip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pluginArchive))) {
      zip.putNextEntry(new ZipEntry("archive-root/"));
      zip.putNextEntry(new ZipEntry("archive-root/lib/"));
      zip.putNextEntry(new ZipEntry("archive-root/lib/plugin.jar"));
      zip.write(0);
      zip.closeEntry();
    }
    Path pluginsPath = intellij.getPluginsInstallationPath();
    context.getFileAccess().mkdirs(pluginsPath);

    // act
    downloader.extractDownloadedPlugin(context.getFileAccess(), pluginArchive, plugin.id());

    // assert
    Path pluginPath = pluginsPath.resolve(plugin.id());
    assertThat(pluginPath.resolve("lib").resolve("plugin.jar")).exists();
    assertThat(pluginsPath.resolve("archive-root")).doesNotExist();

    // act
    intellij.uninstallPlugin(plugin);

    // assert
    assertThat(pluginPath).doesNotExist();
  }
}
