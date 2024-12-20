package com.devonfw.tools.ide.repo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.os.SystemInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link CustomToolsJson} for the ide-custom-tools.json file.
 */
public record CustomToolsJson(@JsonIgnore String title, String url, List<CustomTool> tools) {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Retrieves custom tools from a devonfw-ide legacy config.
   *
   * @param customToolsContent String of custom tools
   * @param context the {@link IdeContext}.
   * @return {@link CustomToolsJson}.
   */
  public static CustomToolsJson retrieveCustomToolsFromLegacyConfig(String customToolsContent, IdeContext context) {
    List<String> tools = VariableLine.fromString(customToolsContent, true);

    if (!tools.isEmpty()) {
      List<CustomTool> customToolJsonList = new ArrayList<>();
      Tool toolObject = parseTool(tools.get(0), "");
      String defaultUrl = "";
      if (toolObject != null) {
        defaultUrl = toolObject.url();
      }

      if (!defaultUrl.isEmpty()) {
        for (String tool : tools) {
          customToolJsonList.add(createCustomToolFromString(tool, defaultUrl, context.getSystemInfo()));
        }

        return new CustomToolsJson("tools", defaultUrl, customToolJsonList);
      }

    }
    return null;
  }

  // TODO: move to separate file
  private record Tool(String name, String version, boolean osAgnostic, boolean archAgnostic, String url) {

  }

  private static CustomTool createCustomToolFromString(String tool, String defaultUrl, SystemInfo systemInfo) {
    Tool toolObject = parseTool(tool, defaultUrl);
    String toolName = "";
    String version = "";
    boolean osAgnostic = false;
    boolean archAgnostic = false;
    String url = "";
    if (toolObject != null) {
      toolName = toolObject.name();
      version = toolObject.version();
      osAgnostic = toolObject.osAgnostic();
      archAgnostic = toolObject.archAgnostic();
      url = toolObject.url();
    }
    String checksum = "";

    return new CustomTool(toolName, version, osAgnostic, archAgnostic, url, checksum,
        systemInfo);
  }

  private static Tool parseTool(String tool, String defaultUrl) {
    int firstColon = tool.indexOf(":");
    if (firstColon < 0) {
      return null;
    }
    String toolName = tool.substring(0, firstColon);
    int secondColon = tool.indexOf(":", firstColon + 1);
    if (secondColon < 0) {
      return null;
    }
    String version = tool.substring(firstColon + 1, secondColon);
    int thirdColon = tool.indexOf(":", secondColon + 1);
    boolean osAgnostic = false;
    boolean archAgnostic = false;
    if (thirdColon < 0) {
      return new Tool(toolName, version, osAgnostic, archAgnostic, defaultUrl);
    }
    String isAgnostic = tool.substring(secondColon + 1, thirdColon);
    if (isAgnostic.equals("all")) {
      osAgnostic = true;
      archAgnostic = true;
    }
    int fourthColon = tool.indexOf(":", thirdColon + 1);
    if (fourthColon < 0) {
      return null;
    }
    String url = parseToolUrl(tool, thirdColon + 1);
    if (url == null) {
      url = defaultUrl;
    }

    return new Tool(toolName, version, osAgnostic, archAgnostic, url);
  }

  private static String parseToolUrl(String tool, int index) {
    String url = tool.substring(index);
    if (url.isEmpty()) {
      return null;
    }
    return url;
  }


  public void doSave(Path path) {

    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, this);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save file " + path, e);
    }
  }

  public CustomToolsJson doLoad(Path path) {
    CustomToolsJson customToolsJson = null;
    if (Files.exists(path)) {
      try (BufferedReader reader = Files.newBufferedReader(path)) {
        customToolsJson = MAPPER.readValue(reader, CustomToolsJson.class);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + path, e);
      }
    }
    return customToolsJson;
  }

}
