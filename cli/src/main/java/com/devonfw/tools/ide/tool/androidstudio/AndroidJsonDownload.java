package com.devonfw.tools.ide.tool.androidstudio;

import com.devonfw.tools.ide.common.JsonObject;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a download of Android. We map only properties that we are interested in and let jackson ignore
 * all others.
 */
public class AndroidJsonDownload implements JsonObject {

  @JsonProperty("link")
  private String link;

  @JsonProperty("checksum")
  private String checksum;

  /**
   * @return link
   */
  public String getLink() {

    return this.link;
  }

  /**
   * @return checksum
   */
  public String getChecksum() {

    return this.checksum;
  }

}
