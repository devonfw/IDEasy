package com.devonfw.tools.ide.github;

import com.devonfw.tools.ide.json.JsonVersionItem;

/**
 * JSON data object for a github release.
 *
 * @param name the official name of the release as its version.
 */
public record GithubRelease(String name) implements JsonVersionItem {

  @Override
  public String version() {
    return name();
  }
}
