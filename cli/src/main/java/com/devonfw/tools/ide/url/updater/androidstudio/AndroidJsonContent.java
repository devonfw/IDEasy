package com.devonfw.tools.ide.url.updater.androidstudio;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a content of Android. We map only properties that we are interested in and let jackson ignore
 * all others.
 */
public class AndroidJsonContent {

  @JsonProperty("item")
  private List<AndroidJsonItem> item;

  /**
   * @return the {@link List} of {@link AndroidJsonItem}s.
   */
  public List<AndroidJsonItem> getItem() {

    return this.item;
  }
}