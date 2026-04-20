package com.devonfw.tools.ide.url.tool.java;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a version of Java. We map only properties that we are interested in and let jackson ignore all others.
 *
 * @see JavaJsonObject#versions()
 */
public record JavaJsonVersion(@JsonProperty("openjdk_version") String openjdkVersion, @JsonProperty("semver") String semver) implements JsonVersionItem {

  @Override
  public String version() {
    String version = openjdkVersion();
    version = version.replace("+", "_");
    // replace 1.8.0_ to 8u
    if (version.startsWith("1.8.0_")) {
      version = "8u" + version.substring(6);
      version = version.replace("-b", "b");
    }
    return version;
  }
}
