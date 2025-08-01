package com.devonfw.tools.ide.tool.repository;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link CustomToolRepository}.
 */
public class CustomToolRepositoryImpl extends AbstractToolRepository implements CustomToolRepository {

  private final String id;

  private final Map<String, CustomToolMetadata> toolsMap;

  private final Collection<CustomToolMetadata> tools;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   * @param tools the {@link CustomToolMetadata}s.
   */
  public CustomToolRepositoryImpl(IdeContext context, Collection<CustomToolMetadata> tools) {

    super(context);
    this.toolsMap = new HashMap<>(tools.size());
    String repoId = null;
    for (CustomToolMetadata tool : tools) {
      String name = tool.getTool();
      CustomToolMetadata duplicate = this.toolsMap.put(name, tool);
      if (duplicate != null) {
        throw new IllegalStateException("Duplicate custom tool '" + name + "'!");
      }
      if (repoId == null) {
        repoId = computeId(tool.getRepositoryUrl());
      }
    }
    if (repoId == null) {
      repoId = "custom";
    }
    this.id = repoId;
    this.tools = Collections.unmodifiableCollection(this.toolsMap.values());
  }

  private static String computeId(String url) {

    String id = url;
    int schemaIndex = id.indexOf("://");
    if (schemaIndex > 0) {
      id = id.substring(schemaIndex + 3); // remove schema like "https://"
      id = URLDecoder.decode(id, StandardCharsets.UTF_8);
    }
    id = id.replace('\\', '/').replace("//", "/"); // normalize slashes
    if (id.startsWith("/")) {
      id = id.substring(1);
    }
    StringBuilder sb = new StringBuilder(id.length());
    if (schemaIndex > 0) { // was a URL?
      int slashIndex = id.indexOf('/');
      if (slashIndex > 0) {
        sb.append(id.substring(0, slashIndex).replace(':', '_'));
        sb.append('/');
        id = id.substring(slashIndex + 1);
      }
    }
    int length = id.length();
    for (int i = 0; i < length; i++) {
      char c = id.charAt(i);
      if (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')) || (c == '.')
          || (c == '-')) {
        sb.append(c);
      } else {
        sb.append('_');
      }
    }
    return sb.toString();
  }

  @Override
  public String getId() {

    return this.id;
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    CustomToolMetadata customTool = getCustomTool(tool);
    if (!version.equals(customTool.getVersion())) {
      throw new IllegalArgumentException("Undefined version '" + version + "' for custom tool '" + tool
          + "' - expected version '" + customTool.getVersion() + "'!");
    }
    if (!edition.equals(customTool.getEdition())) {
      throw new IllegalArgumentException("Undefined edition '" + edition + "' for custom tool '" + tool + "'!");
    }
    return customTool;
  }

  private CustomToolMetadata getCustomTool(String tool) {
    CustomToolMetadata customTool = this.toolsMap.get(tool);
    if (customTool == null) {
      throw new IllegalArgumentException("Undefined custom tool '" + tool + "'!");
    }
    return customTool;
  }

  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet) {

    CustomToolMetadata customTool = getCustomTool(tool);
    VersionIdentifier customToolVersion = customTool.getVersion();
    if (!version.contains(customToolVersion)) {
      throw new IllegalStateException(customTool + " does not satisfy version to install " + version);
    }
    return customToolVersion;
  }

  @Override
  public List<VersionIdentifier> getSortedVersions(String tool, String edition, ToolCommandlet toolCommandlet) {

    CustomToolMetadata customTool = getCustomTool(tool);
    return List.of(customTool.getVersion());
  }

  @Override
  public Collection<CustomToolMetadata> getTools() {

    return this.tools;
  }

  @Override
  public Collection<ToolDependency> findDependencies(String tool, String edition, VersionIdentifier version) {

    return Collections.emptyList();
  }

  @Override
  public ToolSecurity findSecurity(String tool, String edition) {
    return ToolSecurity.getEmpty();
  }

  @Override
  public List<String> getSortedEditions(String tool) {

    return List.of(tool);
  }

  /**
   * @param context the owning {@link IdeContext}.
   * @return the {@link CustomToolRepository}.
   */
  public static CustomToolRepository of(IdeContext context) {

    Path settingsPath = context.getSettingsPath();
    Path customToolsJsonFile = null;
    if (settingsPath != null) {
      customToolsJsonFile = settingsPath.resolve(IdeContext.FILE_CUSTOM_TOOLS);
    }
    List<CustomToolMetadata> tools;
    if ((customToolsJsonFile != null) && Files.exists(customToolsJsonFile)) {
      CustomToolsJson customToolsJson = CustomToolsJsonMapper.loadJson(customToolsJsonFile);
      tools = CustomToolsJsonMapper.convert(customToolsJson, context);
    } else {
      tools = new ArrayList<>();
    }
    return new CustomToolRepositoryImpl(context, tools);
  }

}
