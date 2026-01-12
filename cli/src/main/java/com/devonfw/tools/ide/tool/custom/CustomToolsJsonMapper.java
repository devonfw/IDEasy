package com.devonfw.tools.ide.tool.custom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.SystemInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mapper of {@link CustomToolsJson} from/to JSON.
 */
public class CustomToolsJsonMapper {

  private static final ObjectMapper MAPPER = JsonMapping.create();

  /**
   * @param customTools the {@link CustomToolsJson} to save.
   * @param path the {@link Path} of the file where to save as JSON.
   */
  public static void saveJson(CustomToolsJson customTools, Path path) {

    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, customTools);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save file " + path, e);
    }
  }

  /**
   * @param path the {@link Path} of the JSON file to load.
   * @return the parsed {@link CustomToolsJson} or {@code null} if the {@link Path} is not an existing file.
   */
  public static CustomToolsJson loadJson(Path path) {
    CustomToolsJson customToolsJson = null;
    if (Files.isRegularFile(path)) {
      try (BufferedReader reader = Files.newBufferedReader(path)) {
        customToolsJson = MAPPER.readValue(reader, CustomToolsJson.class);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + path, e);
      }
    }
    return customToolsJson;
  }

  /**
   * @param customTools the {@link CustomToolsJson} to convert.
   * @param context the {@link IdeContext}.
   * @return the converted {@link List} of {@link CustomToolMetadata}.
   */
  public static List<CustomToolMetadata> convert(CustomToolsJson customTools, IdeContext context) {

    String repositoryUrl = customTools.url();
    List<CustomToolJson> tools = customTools.tools();
    List<CustomToolMetadata> result = new ArrayList<>(tools.size());
    for (CustomToolJson customTool : tools) {
      result.add(convert(customTool, repositoryUrl, context));
    }
    return result;
  }

  private static CustomToolMetadata convert(CustomToolJson customTool, String repositoryUrl, IdeContext context) {

    String tool = customTool.name();
    String version = customTool.version();
    SystemInfo systemInfo = context.getSystemInfo();
    String repoUrl = customTool.url();
    if ((repoUrl == null) || repoUrl.isEmpty()) {
      repoUrl = repositoryUrl;
    }
    int capacity = repoUrl.length() + 2 * tool.length() + 2 * version.length() + 7;
    OperatingSystem os;
    if (customTool.osAgnostic()) {
      os = null;
    } else {
      os = systemInfo.getOs();
      capacity += os.toString().length() + 1;
    }
    SystemArchitecture arch;
    if (customTool.archAgnostic()) {
      arch = null;
    } else {
      arch = systemInfo.getArchitecture();
      capacity += arch.toString().length() + 1;
    }
    StringBuilder sb = new StringBuilder(capacity);
    sb.append(repoUrl);
    char last = repoUrl.charAt(repoUrl.length() - 1);
    if ((last != '/') && (last != '\\')) {
      sb.append('/');
    }
    sb.append(tool);
    sb.append('/');
    sb.append(version);
    sb.append('/');
    sb.append(tool);
    sb.append('-');
    sb.append(version);
    if (os != null) {
      sb.append('-');
      sb.append(os);
    }
    if (arch != null) {
      sb.append('-');
      sb.append(arch);
    }
    sb.append(".tgz");
    String url = sb.toString();
    return new CustomToolMetadata(tool, version, os, arch, url, null, repoUrl);
  }

  /**
   * Retrieves custom tools from a devonfw-ide legacy config.
   *
   * @param customToolsContent String of custom tools
   * @param context the {@link IdeContext}.
   * @return {@link CustomToolsJson}.
   */
  public static CustomToolsJson parseCustomToolsFromLegacyConfig(String customToolsContent, IdeContext context) {
    List<String> customToolEntries = VariableLine.parseArray(customToolsContent);
    if (customToolEntries.isEmpty()) {
      return null;
    }
    List<CustomToolJson> customTools = new ArrayList<>(customToolEntries.size());
    String defaultUrl = null;
    for (String customToolConfig : customToolEntries) {
      CustomToolJson customToolJson = parseCustomToolFromLegacyConfig(customToolConfig);
      if (customToolJson == null) {
        context.warning("Invalid custom tool entry: {}", customToolConfig);
      } else {
        String url = customToolJson.url();
        if (defaultUrl == null) {
          if ((url == null) || url.isEmpty()) {
            context.warning("First custom tool entry has no URL specified: {}", customToolConfig);
          } else {
            defaultUrl = url;
            customToolJson = customToolJson.withoutUrl();
          }
        } else if (defaultUrl.equals(url)) {
          customToolJson = customToolJson.withoutUrl();
        }
        customTools.add(customToolJson);
      }
    }
    if (customTools.isEmpty() || (defaultUrl == null)) {
      return null;
    }
    return new CustomToolsJson(defaultUrl, customTools);
  }

  private static CustomToolJson parseCustomToolFromLegacyConfig(String customToolConfig) {
    int firstColon = customToolConfig.indexOf(":");
    if (firstColon < 0) {
      return null;
    }
    String toolName = customToolConfig.substring(0, firstColon);
    int secondColon = customToolConfig.indexOf(":", firstColon + 1);
    if (secondColon < 0) {
      return null;
    }
    String version = customToolConfig.substring(firstColon + 1, secondColon);
    int thirdColon = customToolConfig.indexOf(":", secondColon + 1);
    boolean osAgnostic = false;
    boolean archAgnostic = false;
    String url = null;
    if (thirdColon > 0) {
      if (customToolConfig.substring(secondColon + 1, thirdColon).equals("all")) {
        osAgnostic = true;
        archAgnostic = true;
        url = customToolConfig.substring(thirdColon + 1);
      } else {
        url = customToolConfig.substring(secondColon + 1);
      }
    }
    return new CustomToolJson(toolName, version, osAgnostic, archAgnostic, url);
  }

}
