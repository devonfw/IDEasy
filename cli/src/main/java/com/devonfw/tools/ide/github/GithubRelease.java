package com.devonfw.tools.ide.github;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a github release ref.
 */
public record GithubRelease(@JsonProperty("tag_name") String ref) implements JsonVersionItem {

  @Override
  public String version() {
    //todo: check if this was a good idea...
    // using @JsonProperty("name") creates conflict for versions such as "9.4.0 RC2" because of the blank space
    // using @JsonProperty("tag_name") creates conflicts due to the "v" in front of the version, e.g. "v9.4.0-RC2"
    // implemented workaround: use tag_name and replace the v

    return ref().replace("v", "");
  }
}
