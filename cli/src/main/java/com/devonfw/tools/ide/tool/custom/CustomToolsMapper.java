package com.devonfw.tools.ide.tool.custom;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.json.StandardJsonObjectMapper;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.SystemInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mapper of {@link CustomTools} from/to JSON or legacy properties.
 */
public class CustomToolsMapper extends StandardJsonObjectMapper<CustomTools> {

  private static final Logger LOG = LoggerFactory.getLogger(CustomToolsMapper.class);

  private static final CustomToolsMapper INSTANCE = new CustomToolsMapper();

  private final ObjectMapper MAPPER = JsonMapping.create();

  private CustomToolsMapper() {
    super(CustomTools.class);
  }

  @Override
  public String getStandardFilename() {

    return "ide-custom-tools.json";
  }

  /**
   * @param customTools the {@link CustomTools} to convert.
   * @param context the {@link IdeContext}.
   * @return the converted {@link List} of {@link CustomToolMetadata}.
   */
  public static List<CustomToolMetadata> convert(CustomTools customTools, IdeContext context) {

    String repositoryUrl = customTools.url();
    List<CustomTool> tools = customTools.tools();
    List<CustomToolMetadata> result = new ArrayList<>(tools.size());
    for (CustomTool customTool : tools) {
      result.add(convert(customTool, repositoryUrl, context));
    }
    return result;
  }

  private static CustomToolMetadata convert(CustomTool customTool, String repositoryUrl, IdeContext context) {

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
   * @return {@link CustomTools}.
   */
  public static CustomTools parseCustomToolsFromLegacyConfig(String customToolsContent) {
    List<String> customToolEntries = VariableLine.parseArray(customToolsContent);
    if (customToolEntries.isEmpty()) {
      return null;
    }
    List<CustomTool> customTools = new ArrayList<>(customToolEntries.size());
    String defaultUrl = null;
    for (String customToolConfig : customToolEntries) {
      CustomTool customTool = parseCustomToolFromLegacyConfig(customToolConfig);
      if (customTool == null) {
        LOG.warn("Invalid custom tool entry: {}", customToolConfig);
      } else {
        String url = customTool.url();
        if (defaultUrl == null) {
          if ((url == null) || url.isEmpty()) {
            LOG.warn("First custom tool entry has no URL specified: {}", customToolConfig);
          } else {
            defaultUrl = url;
            customTool = customTool.withoutUrl();
          }
        } else if (defaultUrl.equals(url)) {
          customTool = customTool.withoutUrl();
        }
        customTools.add(customTool);
      }
    }
    if (customTools.isEmpty() || (defaultUrl == null)) {
      return null;
    }
    return new CustomTools(defaultUrl, customTools);
  }

  private static CustomTool parseCustomToolFromLegacyConfig(String customToolConfig) {
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
    return new CustomTool(toolName, version, osAgnostic, archAgnostic, url);
  }

  /**
   * @return the singleton instance of {@link CustomToolsMapper}.
   */
  public static CustomToolsMapper get() {

    return INSTANCE;
  }

}
