package com.devonfw.tools.ide.url.updater.python;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Java object to represent the JSON of a Python release. Mapping just the needed Properties
 */
public class PythonRelease {

  @JsonProperty("version")
  private String version;

  @JsonProperty("files")
  private List<PythonFile> files;

  /**
   * @return the version of the Python release.
   */
  public String getVersion() {

    return this.version;
  }

  /**
   * @return the {@link List} of {@link PythonFile}s.
   */
  public List<PythonFile> getFiles() {

    return this.files;
  }
}
