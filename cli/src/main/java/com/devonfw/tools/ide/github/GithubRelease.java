package com.devonfw.tools.ide.github;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a github release ref.
 */
public record GithubRelease(@JsonProperty("tag_name") String ref) implements JsonVersionItem {

  @Override
  public String version() {

    return ref().replace("v", "");
  }
}
