package com.devonfw.tools.ide.url.updater.intellij;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for an item of Intellij. We map only properties that we are interested in and let jackson ignore all
 * others.
 */
public class IntellijJsonDownloads {

  @JsonProperty("downloads")
  private List<IntellijJsonDownloadsItem> downloads;

  /**
   * @return the {@link List} of {@link IntellijJsonDownloadsItem}s.
   */
  public List<IntellijJsonDownloadsItem> getDownloads() {

    return this.downloads;
  }

}
