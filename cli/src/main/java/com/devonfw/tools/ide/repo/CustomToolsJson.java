package com.devonfw.tools.ide.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link CustomToolsJson} for the ide-custom-tools.json file.
 */
public record CustomToolsJson(String title, String url, List<CustomTool> tools) {

  /**
   * Retrieves custom tools from a devonfw-ide legacy config.
   *
   * @param customToolsContent String of custom tools
   * @param context the {@link IdeContext}.
   * @return {@link CustomToolsJson}.
   */
  public static CustomToolsJson retrieveCustomToolsFromLegacyConfig(String customToolsContent, IdeContext context) {
    String toolsString = extractToolsString(customToolsContent);
    if (!toolsString.isEmpty()) {
      String[] tools = toolsString.split(" ");
      List<CustomTool> customToolJsonList = new ArrayList<>();

      String defaultUrl = retrieveUrlFromTool(tools[0]);

      if (!defaultUrl.isEmpty()) {
        for (String tool : tools) {
          customToolJsonList.add(createCustomToolFromString(tool, defaultUrl, context.getSystemInfo()));
        }

        return new CustomToolsJson("tools", defaultUrl, customToolJsonList);
      }

    }
    return null;
  }

  private static CustomTool createCustomToolFromString(String tool, String defaultUrl, SystemInfo systemInfo) {
    String[] parts = tool.split(":");
    String name = parts[0];
    String version = parts[1];
    boolean osAgnostic = isAgnostic(parts);
    boolean archAgnostic = isAgnostic(parts);
    String url;
    if (parts.length > 3) {
      url = retrieveUrlFromTool(tool);
    } else {
      url = defaultUrl;
    }
    String checksum = "";
    return new CustomTool(name, VersionIdentifier.of(version), osAgnostic, archAgnostic, url, checksum,
        systemInfo);
  }

  private static boolean isAgnostic(String[] parts) {
    if (parts.length > 2 && "all".equals(parts[2])) {
      return true;
    }
    return false;
  }

  private static String extractToolsString(String input) {
    Pattern pattern = Pattern.compile("\\((.*?)\\)");
    Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }

  private static String retrieveUrlFromTool(String toolString) {
    String[] parts = toolString.split(":");
    String url = "";
    if (parts.length > 3) {
      String regex = "(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(toolString);
      if (matcher.find()) {
        url = matcher.group(0);
      }
    }
    return url;
  }

}
