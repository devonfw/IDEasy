package com.devonfw.tools.ide.github;

import com.devonfw.tools.ide.json.JsonVersionItem;

/**
 * JSON data object for a github release.
 *
 * @param name the official name of the release as its version.
 * @param prerelease whether this is a pre-release version.
 */
public record GithubRelease(String name, boolean prerelease) implements JsonVersionItem {

  /**
   * Constructor for backwards compatibility with name only (not a prerelease).
   * @param name the official name of the release as its version.
   */
  public GithubRelease(String name) {
    this(name, false);
  }

  @Override
  public String version() {
    return name();
  }
}
