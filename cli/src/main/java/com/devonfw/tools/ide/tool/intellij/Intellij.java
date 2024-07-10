package com.devonfw.tools.ide.tool.intellij;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;

import java.io.IOException;
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
      throw new IllegalStateException("Failed to read Intellij build Version: " + buildFile, e);
    }

    String fileName = String.format("intellij-plugin-%s-%s.zip", buildVersion, pluginId);

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
      fileAccess.download(downloadUrl, tmpDir.resolve(fileName));
      Path targetDir = this.getPluginsInstallationPath().resolve(pluginId);
      fileAccess.extract(tmpDir.resolve(fileName), targetDir);

      if (tmpDir != null) {
        fileAccess.delete(tmpDir);
      }

    }

    //    Path targetFile = this.getToolPath().resolve("plugins").resolve("mocked.zip");
    //    Path targetDirectory = this.getToolPath().resolve("plugins").resolve("mocked");
    //    this.context.getFileAccess().download(plugin.getUrl(), targetFile);
    //    this.context.getFileAccess().extract(targetFile, targetDirectory);
    //
    //    // TODO Auto-generated method stub
    //    this.context.info("Installplugin" + plugin.getUrl());

    // download and install the global tool
    //    FileAccess fileAccess = this.context.getFileAccess();
    //    Path target = this.getToolPath().resolve("plugins").resolve("mocked.zip");
    //    Path executable = target;
    //    Path tmpDir = null;
    //    boolean extract = isExtract();
    //    if (extract) {
    //      tmpDir = fileAccess.createTempDir(getName());
    //      Path downloadBinaryPath = tmpDir.resolve(target.getFileName());
    //      fileAccess.extract(target, downloadBinaryPath);
    //      executable = fileAccess.findFirst(downloadBinaryPath, Files::isExecutable, false);
    //    }
    //    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(executable);
    //    int exitCode = pc.run();
    //    if (tmpDir != null) {
    //      fileAccess.delete(tmpDir);
    //    }
    //    if (exitCode == 0) {
    //      this.context.success("Successfully installed {} in version {}", this.tool);
    //    } else {
    //      this.context.warning("{} in version {} was not successfully installed", this.tool);
    //      //return false;
    //    }

  }

}