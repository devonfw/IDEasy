package com.devonfw.tools.ide.url.updater.java;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a version of of Java. We map only properties that we are interested in and let jackson ignore
 * all others.
 *
 * @see JavaJsonObject#getVersions()
 */
public class JavaJsonVersion {
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
}