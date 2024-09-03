package com.devonfw.tools.ide.tool.vscode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginDescriptor;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends IdeToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Vscode(IdeContext context) {

    super(context, "vscode", Set.of(Tag.VS_CODE));
  }

  @Override
  protected String getBinaryName() {

    return "code";
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }

  @Override
  protected void installPlugins(Collection<PluginDescriptor> plugins) {

    List<PluginDescriptor> pluginsToInstall = new ArrayList<>();
    List<String> pluginsToRecommend = new ArrayList<>();

    for (PluginDescriptor plugin : plugins) {
      if (plugin.isActive()) {
        pluginsToInstall.add(plugin);
      } else {
        pluginsToRecommend.add(plugin.getId());
      }
    }
    doAddRecommendations(pluginsToRecommend.toArray(new String[0]));
    doInstallPlugins(pluginsToInstall);

  }

  private void doInstallPlugins(List<PluginDescriptor> pluginsToInstall) {
    if (pluginsToInstall.isEmpty()) {
      this.context.info("No plugins to be installed");
    } else {
      for (PluginDescriptor plugin : pluginsToInstall) {
        this.arguments.addValue("--install-extension");
        this.arguments.addValue(plugin.getId());
      }
    }
  }

  private void doAddRecommendations(String... recommendations) {
    Path extensionsJsonPath = this.context.getWorkspacePath().resolve(".vscode/extensions.json");
    String recommendationsKey = "\"recommendations\": [" + String.join(", ", recommendations) + "]";

    try {
      if (Files.exists(extensionsJsonPath) && Files.size(extensionsJsonPath) > 0) {
        String content = Files.readString(extensionsJsonPath, StandardCharsets.UTF_8);

        if (!content.contains(recommendationsKey)) {
          content = content.replaceAll("\"recommendations\": \\[[^\\]]*\\]", recommendationsKey);
          Files.writeString(extensionsJsonPath, content, StandardCharsets.UTF_8);
          this.context.success("Plugin recommendations have been updated in " + extensionsJsonPath);
        } else {
          this.context.success("All plugin recommendations are up to date.");
        }

      } else {
        Files.writeString(extensionsJsonPath, "{" + recommendationsKey + "}", StandardCharsets.UTF_8);
        this.context.success("Plugin recommendations have been created in " + extensionsJsonPath);
      }
    } catch (IOException e) {
      this.context.error(e);
    }


  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    install(true);

    Path vsCodeConf = this.context.getWorkspacePath().resolve(".vscode/.userdata");

    if (!Files.exists(vsCodeConf)) {
      Path devonConfDir = this.context.getIdeHome().resolve("conf/vscode");

      if (Files.exists(devonConfDir)) {
        this.context.info("Migrating " + devonConfDir.toAbsolutePath() + " to " + vsCodeConf.toAbsolutePath());
        this.context.info("For details see https://github.com/devonfw/ide/issues/553");

        try {
          copyDirectory(devonConfDir, vsCodeConf);
        } catch (IOException e) {
          this.context.error("Error copying directories: " + e);
        }
      } else {
        try {
          Files.createDirectories(vsCodeConf);
        } catch (IOException e) {
          this.context.error("Failed to create directories for vscode configuration: " + e);
        }
      }
    }

    Path vsCodeExtensionFolder = this.context.getIdeHome().resolve("plugins/vscode");
    try {
      Files.createDirectories(vsCodeExtensionFolder);
    } catch (IOException e) {
      this.context.error("Failed to create directories for vscode extensions: " + e);
    }

    Path binaryPath;
    binaryPath = Path.of(getBinaryName());

    List<String> command = new ArrayList<>();
    command.add("--new-window");
    command.add("--user-data-dir=" + vsCodeConf);
    command.add("--extensions-dir=" + vsCodeExtensionFolder);

    String[] extensions = this.arguments.asArray();
    command.addAll(Arrays.asList(extensions));

    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW).executable(binaryPath).addArgs(command.toArray());
    pc.run(processMode);
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    Files.walk(source).forEach(src -> {
          Path destination = target.resolve(source.relativize(src));
          try {
            Files.copy(src, destination, StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException e) {
            this.context.error("Error copying: " + e);
          }
        }
    );
  }

}
