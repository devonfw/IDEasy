package com.devonfw.tools.ide.tool.vscode;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
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
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {

    List<String> extensionsCommands = new ArrayList<>();
    extensionsCommands.add("--force");
    extensionsCommands.add("--install-extension");
    extensionsCommands.add(plugin.id());
    ProcessResult result = runTool(ProcessMode.DEFAULT, ProcessErrorHandling.THROW_ERR, pc, extensionsCommands.toArray(String[]::new));
    if (result.isSuccessful()) {
      this.context.success("Successfully installed plugin: {}", plugin.name());
      step.success();
      return true;
    } else {
      this.context.warning("An error occurred while installing plugin: {}", plugin.name());
      return false;
    }
  }

  @Override
  protected void installPlugins(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {

    List<ToolPluginDescriptor> pluginsToInstall = new ArrayList<>();
    List<ToolPluginDescriptor> pluginsToRecommend = new ArrayList<>();

    Step step = this.context.newStep(true, "Install plugins for " + this.tool);
    try {
      for (ToolPluginDescriptor plugin : plugins) {
        if (plugin.active()) {
          if (retrievePluginMarkerFilePath(plugin) != null && Files.exists(retrievePluginMarkerFilePath(plugin))) {
            this.context.debug("Markerfile for IDE: {} and active plugin: {} already exists.", getName(), plugin.name());
          } else {
            pluginsToInstall.add(plugin);
          }
        } else {
          pluginsToRecommend.add(plugin);
        }
      }
      if (pluginsToInstall.isEmpty()) {
        this.context.debug("No plugins to be installed");
        step.success();
      } else {
        for (ToolPluginDescriptor plugin : pluginsToInstall) {
          boolean result = installPlugin(plugin, step, pc);
          if (result) {
            createPluginMarkerFile(plugin);
          }
        }
      }
      doAddRecommendations(pluginsToRecommend);
    } catch (RuntimeException e) {
      step.error(e, true);
      throw e;
    } finally {
      step.close();
    }

  }

  private void doAddRecommendations(List<ToolPluginDescriptor> recommendations) {
    Path extensionsJsonPath = this.context.getWorkspacePath().resolve(".vscode/extensions.json");

    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> recommendationsMap;

    if (Files.exists(extensionsJsonPath)) {
      try (BufferedReader reader = Files.newBufferedReader(extensionsJsonPath)) {
        recommendationsMap = objectMapper.readValue(reader, Map.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      recommendationsMap = new HashMap<>();
    }

    List<String> existingRecommendations = (List<String>) recommendationsMap.getOrDefault("recommendations", new ArrayList<>());

    try {
      int addedRecommendations = 0;
      Set<String> existingRecommendationsSet = new HashSet<>(existingRecommendations);
      for (ToolPluginDescriptor recommendation : recommendations) {
        String recommendationId = recommendation.id();
        if (existingRecommendationsSet.add(recommendationId)) {
          existingRecommendations.add(recommendationId);
          addedRecommendations++;
        }
      }

      if (addedRecommendations > 0) {
        objectMapper.writeValue(extensionsJsonPath.toFile(), recommendationsMap);
      }

    } catch (IOException e) {
      this.context.error(e);
    }
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, ProcessErrorHandling errorHandling, String... args) {

    Path vsCodeConf = this.context.getWorkspacePath().resolve(".vscode/.userdata");
    pc.addArg("--new-window");
    pc.addArg("--user-data-dir=" + vsCodeConf);
    Path vsCodeExtensionFolder = this.context.getIdeHome().resolve("plugins/vscode");
    pc.addArg("--extensions-dir=" + vsCodeExtensionFolder);
    pc.addArg(this.context.getWorkspacePath());
    super.configureToolArgs(pc, processMode, errorHandling, args);
  }

}
