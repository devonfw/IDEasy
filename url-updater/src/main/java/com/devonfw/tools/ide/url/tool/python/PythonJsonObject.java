package com.devonfw.tools.ide.url.tool.python;

import java.util.List;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * Java object to represent the JSON of Python release information. This is the root {@link JsonObject}.
 */
public class PythonJsonObject implements JsonObject {

  private List<PythonRelease> releases;

  /**
   * @param releases new list of {@link PythonRelease}s
   */
  public void setReleases(List<PythonRelease> releases) {

    this.releases = releases;
  }

  /**
   * @return the {@link List} of {@link PythonRelease}s.
   */
  public List<PythonRelease> getReleases() {

    return this.releases;
  }
}
