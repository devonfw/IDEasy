package com.devonfw.tools.ide.url.tool.java;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a version of Java from Azul. We map only properties that we are interested in and let jackson ignore all others.
 *
 * @param javaVersion
 */
public record JavaAzulJsonVersion(@JsonProperty("java_version") int[] javaVersion) implements JsonVersionItem {

  @Override
  public String version() {
    StringBuilder version = new StringBuilder();
    for (int i = 0; i < javaVersion.length; i++) {
      version.append(javaVersion[i]);
      if (i < javaVersion.length - 1) {
        version.append(".");
      }
    }
    return version.toString();
  }


}
