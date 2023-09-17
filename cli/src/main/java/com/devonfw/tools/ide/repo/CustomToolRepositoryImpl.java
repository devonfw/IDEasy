package com.devonfw.tools.ide.repo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link CustomToolRepository}.
 */
public class CustomToolRepositoryImpl extends AbstractToolRepository implements CustomToolRepository {

  private final String id;

  private final Map<String, CustomTool> toolsMap;

  private final Collection<CustomTool> tools;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   * @param tools the {@link CustomTool}s.
   */
  public CustomToolRepositoryImpl(IdeContext context, Collection<CustomTool> tools) {

    super(context);
    this.toolsMap = new HashMap<>(tools.size());
    String repoId = null;
    for (CustomTool tool : tools) {
      String name = tool.getTool();
      CustomTool duplicate = this.toolsMap.put(name, tool);
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
    id.replace('\\', '/').replace("//", "/"); // normalize slashes
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
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version) {

    CustomTool customTool = this.toolsMap.get(tool);
    if (customTool == null) {
      throw new IllegalArgumentException("Undefined custom tool '" + tool + "'!");
    }
    if (!version.equals(customTool.getVersion())) {
      throw new IllegalArgumentException("Undefined version '" + version + "' for custom tool '" + tool
          + "' - expected version '" + customTool.getVersion() + "'!");
    }
    if (!edition.equals(customTool.getEdition())) {
      throw new IllegalArgumentException("Undefined edition '" + edition + "' for custom tool '" + tool + "'!");
    }
    return customTool;
  }

  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, VersionIdentifier version) {

    return getMetadata(tool, edition, version).getVersion();
  }

  @Override
  public Collection<CustomTool> getTools() {

    return this.tools;
  }

  /**
   * @param context the owning {@link IdeContext}.
   * @return the {@link CustomToolRepository}.
   */
  public static CustomToolRepository of(IdeContext context) {

    Path settingsPath = context.getSettingsPath();
    Path customToolsJson = null;
    if (settingsPath != null) {
      customToolsJson = settingsPath.resolve(FILE_CUSTOM_TOOLS);
    }
    List<CustomTool> tools = new ArrayList<>();
    if ((customToolsJson != null) && Files.exists(customToolsJson)) {
      try (InputStream in = Files.newInputStream(customToolsJson);
          Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

        JsonReader jsonReader = Json.createReader(new BufferedReader(reader));
        JsonStructure json = jsonReader.read();
        JsonObject jsonRoot = requireObject(json);
        String defaultUrl = getString(jsonRoot, "url", "");
        JsonArray jsonTools = requireArray(jsonRoot.get("tools"));
        for (JsonValue jsonTool : jsonTools) {
          JsonObject jsonToolObject = requireObject(jsonTool);
          String name = getString(jsonToolObject, "name");
          String version = getString(jsonToolObject, "version");
          String url = getString(jsonToolObject, "url", defaultUrl);
          boolean osAgnostic = getBoolean(jsonToolObject, "os-agnostic", Boolean.FALSE);
          boolean archAgnostic = getBoolean(jsonToolObject, "arch-agnostic", Boolean.TRUE);
          if (defaultUrl.isEmpty()) {
            throw new IllegalStateException("Missing 'url' property for tool '" + name + "'!");
          }
          // TODO
          String checksum = null;
          CustomTool customTool = new CustomTool(name, VersionIdentifier.of(version), osAgnostic, archAgnostic, url,
              checksum, context.getSystemInfo());
          tools.add(customTool);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Failed to read JSON from " + customToolsJson, e);
      }
    }
    return new CustomToolRepositoryImpl(context, tools);
  }

  private static boolean getBoolean(JsonObject json, String property, Boolean defaultValue) {

    JsonValue value = json.get(property);
    if (value == null) {
      if (defaultValue == null) {
        throw new IllegalArgumentException("Missing string property '" + property + "' in JSON: " + json);
      }
      return defaultValue.booleanValue();
    }
    ValueType valueType = json.getValueType();
    if (valueType == ValueType.TRUE) {
      return true;
    } else if (valueType == ValueType.FALSE) {
      return false;
    } else {
      throw new IllegalStateException("Expected value type boolean but found " + valueType + " for JSON: " + json);
    }
  }

  private static String getString(JsonObject json, String property) {

    return getString(json, property, null);
  }

  private static String getString(JsonObject json, String property, String defaultValue) {

    JsonValue value = json.get(property);
    if (value == null) {
      if (defaultValue == null) {
        throw new IllegalArgumentException("Missing string property '" + property + "' in JSON: " + json);
      }
      return defaultValue;
    }
    require(json, ValueType.STRING);
    return ((JsonString) json).getString();
  }

  /**
   * @param json the {@link JsonValue} to check.
   */
  private static JsonObject requireObject(JsonValue json) {

    require(json, ValueType.OBJECT);
    return (JsonObject) json;
  }

  /**
   * @param json the {@link JsonValue} to check.
   */
  private static JsonArray requireArray(JsonValue json) {

    require(json, ValueType.ARRAY);
    return (JsonArray) json;
  }

  /**
   * @param json the {@link JsonValue} to check.
   * @param type the expected {@link ValueType}.
   */
  private static void require(JsonValue json, ValueType type) {

    ValueType actualType = json.getValueType();
    if (actualType != type) {
      throw new IllegalStateException(
          "Expected value type " + type + " but found " + actualType + " for JSON: " + json);
    }
  }

}
