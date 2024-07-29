package com.devonfw.tools.ide.tool.java;

import com.devonfw.tools.ide.common.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a version of Java. We map only properties that we are interested in and let jackson ignore all others.
 *
 * @see JavaJsonObject#getVersions()
 */
public class JavaJsonVersion implements JsonVersionItem {

  @JsonProperty("openjdk_version")
  private String openjdkVersion;

  @JsonProperty("semver")
  private String semver;

  /**
   * The constructor.
   */
  public JavaJsonVersion() {

    super();
  }

  /**
   * The constructor.
   *
   * @param openjdkVersion the {@link #getOpenjdkVersion() OpenJDK version}.
   * @param semver the {@link #getSemver() semantic version}.
   */
  public JavaJsonVersion(String openjdkVersion, String semver) {

    super();
    this.openjdkVersion = openjdkVersion;
    this.semver = semver;
  }

  /**
   * @return the OpenJDK version.
   */
  public String getOpenjdkVersion() {

    return this.openjdkVersion;
  }

  /**
   * @param openjdkVersion the new value of {@link #getOpenjdkVersion()}.
   */
  public void setOpenjdkVersion(String openjdkVersion) {

    this.openjdkVersion = openjdkVersion;
  }

  /**
   * @return the semantic version.
   */
  public String getSemver() {

    return this.semver;
  }

  /**
   * @param semver the new value of {@link #getSemver()}.
   */
  public void setSemver(String semver) {

    this.semver = semver;
  }

  @Override
  public String getVersion() {

    String version = getOpenjdkVersion();
    version = version.replace("+", "_");
    // replace 1.8.0_ to 8u
    if (version.startsWith("1.8.0_")) {
      version = "8u" + version.substring(6);
      version = version.replace("-b", "b");
    }
    return version;
  }
}
