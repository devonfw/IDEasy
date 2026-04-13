package com.devonfw.tools.ide.url.tool.java;

import java.util.List;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON item for one Azul Java package release.
 */
public class JavaAzulJsonVersion implements JsonVersionItem {

  private final List<Integer> javaVersion;
  private final Integer buildNumber;
  private final String downloadUrl;

  public JavaAzulJsonVersion(@JsonProperty("java_version") List<Integer> javaVersion, @JsonProperty("openjdk_build_number") Integer buildNumber,
      @JsonProperty("download_url") String downloadUrl) {

    this.javaVersion = javaVersion;
    this.buildNumber = buildNumber;
    this.downloadUrl = downloadUrl;

  }

  public String getDownloadUrl() {
    return this.downloadUrl;
  }

  @Override
  public String version() {

    if (this.javaVersion == null || this.javaVersion.isEmpty()) {
      return null;
    }
    StringBuilder version = new StringBuilder();
    for (int i = 0; i < this.javaVersion.size(); i++) {
      if (i > 0) {
        version.append('.');
      }
      version.append(this.javaVersion.get(i));
    }
    if (this.buildNumber != null) {
      version.append('_').append(this.buildNumber);
    }
    return version.toString();
  }
}
