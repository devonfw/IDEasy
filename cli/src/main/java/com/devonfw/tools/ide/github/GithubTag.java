package com.devonfw.tools.ide.github;

import com.devonfw.tools.ide.json.JsonVersionItem;

/**
 * JSON data object for a github tag ref.
 *
 * @param ref the full tag reference, from which the version is extracted.
 */
public record GithubTag(String ref) implements JsonVersionItem {

  @Override
  public String version() {

    return ref().replace("refs/tags/", "");
  }
}
