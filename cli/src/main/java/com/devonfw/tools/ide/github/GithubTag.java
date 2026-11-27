package com.devonfw.tools.ide.github;

import com.devonfw.tools.ide.json.JsonVersionItem;

/**
 * JSON data object for a GitHub tag.
 *
 * @param name the name of the tag representing the version.
 */
public record GithubTag(String name) implements JsonVersionItem {

  @Override
  public String version() {

    return name();
  }
}
