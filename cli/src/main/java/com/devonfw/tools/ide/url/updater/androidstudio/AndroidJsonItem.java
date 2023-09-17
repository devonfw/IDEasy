package com.devonfw.tools.ide.url.updater.androidstudio;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * JSON data object for an item of Android. We map only properties that we are interested in and let jackson ignore all
 * others.
 */
public class AndroidJsonItem {

  @JsonProperty("version")
  private String version;

  @JsonProperty("download")
  private List<AndroidJsonDownload> download;

  /**
   * @return version
   */
  public String getVersion() {

    return this.version;
  }

  /**
   * @return download
   */
  public List<AndroidJsonDownload> getDownload() {

    return download;
  }

}