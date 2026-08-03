package com.devonfw.tools.ide.tool.custom;

import java.util.List;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link CustomTools} represents the {@code ide-custom-tools.json} file.
 *
 * @param url the repository base URL. This may be a typical URL (e.g. "https://host/path") but may also be a path in your file-system (e.g. to a mounted
 *     remote network drive).
 * @param tools the {@link List} of {@link CustomTool}.
 */
public record CustomTools(String url, List<CustomTool> tools) implements JsonObject {

  /** JSON property name for {@link #url()}. */
  public static final String PROPERTY_URL = "url";

  /** JSON property name for {@link #tools()}. */
  public static final String PROPERTY_TOOLS = "tools";

}
