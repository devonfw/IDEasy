package com.devonfw.tools.ide.github;

import com.devonfw.tools.ide.common.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a github tag ref.
 */
public record GithubTag(@JsonProperty("ref") String ref) implements JsonVersionItem {

  @Override
  public String version() {

    return ref().replace("refs/tags/", "");
  }
}
