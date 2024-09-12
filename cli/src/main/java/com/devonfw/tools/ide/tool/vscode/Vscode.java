package com.devonfw.tools.ide.tool.vscode;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.fasterxml.jackson.databind.ObjectMapper;

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
    List<PluginDescriptor> pluginsToRecommend = new ArrayList<>();

    for (PluginDescriptor plugin : plugins) {
      if (plugin.isActive()) {
        pluginsToInstall.add(plugin);
      } else {
        pluginsToRecommend.add(plugin);
      }
    }
    doAddRecommendations(pluginsToRecommend);
    doInstallPlugins(pluginsToInstall);

  }

  private void doInstallPlugins(List<PluginDescriptor> pluginsToInstall) {

    List<String> extensionsCommands = new ArrayList<>();

    if (pluginsToInstall.isEmpty()) {
      this.context.info("No plugins to be installed");
    } else {

      for (PluginDescriptor plugin : pluginsToInstall) {
        extensionsCommands.add("--install-extension");
        extensionsCommands.add(plugin.getId());
      }
    }
    runTool(ProcessMode.DEFAULT, null, extensionsCommands.toArray(new String[0]));
  }

  private void doAddRecommendations(List<PluginDescriptor> recommendations) {
    Path extensionsJsonPath = this.context.getWorkspacePath().resolve(".vscode/extensions.json");

    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> recommendationsMap;

    try (BufferedReader reader = Files.newBufferedReader(extensionsJsonPath)) {
      recommendationsMap = objectMapper.readValue(reader, Map.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    List<String> existingRecommendations = (List<String>) recommendationsMap.getOrDefault("recommendations", new ArrayList<>());

    try {
      if (Files.exists(extensionsJsonPath) && Files.size(extensionsJsonPath) > 0 && existingRecommendations.size() < recommendations.size()) {

        for (PluginDescriptor recommendation : recommendations) {
          if (!existingRecommendations.contains(recommendation.getId())) {
            existingRecommendations.add(recommendation.getId());
          }
        }

        recommendationsMap.put("recommendations", existingRecommendations);
      } else if (existingRecommendations.size() > recommendations.size()) {
        List<String> newRecommendations = new ArrayList<>();
        recommendationsMap = new HashMap<>();
        for (PluginDescriptor recommendation : recommendations) {
          newRecommendations.add(recommendation.getId());
        }
        recommendationsMap.put("recommendations", newRecommendations);
      } else {
        this.context.info("All recommendations are already set");
      }

      objectMapper.writeValue(extensionsJsonPath.toFile(), recommendationsMap);
    } catch (IOException e) {
      this.context.error(e);
    }
  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    install(true);

    Path vsCodeConf = this.context.getWorkspacePath().resolve(".vscode/.userdata");
    Path vsCodeExtensionFolder = this.context.getIdeHome().resolve("plugins/vscode");

    List<String> command = new ArrayList<>();
    command.add("--new-window");
    command.add("--user-data-dir=" + vsCodeConf);
    command.add("--extensions-dir=" + vsCodeExtensionFolder);

    command.addAll(Arrays.asList(args));

    Path binaryPath;
    binaryPath = Path.of(getBinaryName());
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW).executable(binaryPath).addArgs(command.toArray());
    pc.run(processMode);
  }

}
