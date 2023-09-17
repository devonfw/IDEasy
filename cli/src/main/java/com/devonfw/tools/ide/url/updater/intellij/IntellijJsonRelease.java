package com.devonfw.tools.ide.url.updater.intellij;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for an item of Intellij. We map only properties that we are interested in and let jackson ignore all
 * others.
 */
public class IntellijJsonRelease {

  /** Key for Mac OS on ARM (e.g. M1 or M2 cpu). */
  public static final String KEY_MAC_ARM = "macM1";

  /** Key for Mac OS on x68_64. */
  public static final String KEY_MAC = "mac";

  /** Key for Linux. */
  public static final String KEY_LINUX = "linux";

  /** Key for Windows. */
  public static final String KEY_WINDOWS = "windowsZip";

  @JsonProperty("version")
  private String version;

  @JsonProperty("downloads")
  private Map<String, IntellijJsonDownloadsItem> downloads;

  /** @return the version of the release. */
  public String getVersion() {

    return this.version;
  }

  /**
   * @return a {@link Map} for an OS specific key (see KEY_* constants like {@link #KEY_WINDOWS}) to an
   *         {@link IntellijJsonDownloadsItem}.
   */
  public Map<String, IntellijJsonDownloadsItem> getDownloads() {

    return this.downloads;
  }

}
