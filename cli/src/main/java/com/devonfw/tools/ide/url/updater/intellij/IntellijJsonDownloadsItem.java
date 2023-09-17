package com.devonfw.tools.ide.url.updater.intellij;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * JSON data object for an item of Intellij. We map only properties that we are interested in and let jackson ignore all
 * others.
 */
public class IntellijJsonDownloadsItem {

  /** Key for the URL to download the SHA-256 checksum. */
  public static final String KEY_CHECKSUM_LINK = "checksumLink";

  /** Key for the URL to download the software package. */
  public static final String KEY_LINK = "link";

  @JsonAnySetter
  private Map<String, Object> os_values;

  /**
   * @return the {@link Map} with the properties as key value pairs. Unlike it may be suggested from the name
   *         "os_values" the key is not the operating-system but e.g. {@link #KEY_LINK} or {@link #KEY_CHECKSUM_LINK}.
   */
  public Map<String, Object> getOs_values() {

    return this.os_values;
  }

  /**
   * @param key the key of the requested value.
   * @return the specified value as {@link String}.
   */
  public String getString(String key) {

    Object value = this.os_values.get(key);
    if (value == null) {
      return "";
    }
    return value.toString();
  }

  /**
   * @return the value of {@link #KEY_LINK}.
   */
  public String getLink() {

    return getString(KEY_LINK);
  }

  /**
   * @return the value of {@link #KEY_CHECKSUM_LINK}.
   */
  public String getChecksumLink() {

    return getString(KEY_CHECKSUM_LINK);
  }

}
