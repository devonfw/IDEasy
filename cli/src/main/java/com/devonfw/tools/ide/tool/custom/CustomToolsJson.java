package com.devonfw.tools.ide.tool.custom;

import java.util.List;

/**
 * {@link CustomToolsJson} for the ide-custom-tools.json file.
 *
 * @param url the repository base URL. This may be a typical URL (e.g. "https://host/path") but may also be a path in your file-system (e.g. to a mounted
 *     remote network drive).
 * @param tools the {@link List} of {@link CustomToolJson}.
 */
public record CustomToolsJson(String url, List<CustomToolJson> tools) {

  /** JSON property name for {@link #url()}. */
  public static final String PROPERTY_URL = "url";

  /** JSON property name for {@link #tools()}. */
  public static final String PROPERTY_TOOLS = "tools";

}
