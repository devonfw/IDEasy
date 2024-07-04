package com.devonfw.tools.ide.npm;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a version of Npm. We map only properties that we are interested in and let jackson ignore all
 * others.
 *
 * @see NpmJsonObject#getVersions()
 */
public class NpmJsonDist {

  @JsonProperty("tarball")
  private String tarball;

  public String getTarball() {

    return tarball;
  }

}
