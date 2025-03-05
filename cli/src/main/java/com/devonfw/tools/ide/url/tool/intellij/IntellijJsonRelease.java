package com.devonfw.tools.ide.url.tool.intellij;

import java.util.Map;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for an item of Intellij. We map only properties that we are interested in and let jackson ignore all others.
 */
public record IntellijJsonRelease(@JsonProperty("version") String version,
                                  @JsonProperty("downloads") Map<String, IntellijJsonDownloadsItem> downloads) implements JsonVersionItem {

  /** Key for Mac OS on ARM (e.g. M1 or M2 cpu). */
  public static final String KEY_MAC_ARM = "macM1";

  /** Key for Mac OS on x68_64. */
  public static final String KEY_MAC = "mac";

  /** Key for Linux. */
  public static final String KEY_LINUX = "linux";

  /** Key for Windows. */
  public static final String KEY_WINDOWS = "windowsZip";

}
