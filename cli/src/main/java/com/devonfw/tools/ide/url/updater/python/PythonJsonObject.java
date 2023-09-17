package com.devonfw.tools.ide.url.updater.python;

import java.util.List;

import com.devonfw.tools.ide.common.JsonObject;

/**
 * Java object to represent the JSON of Python release information. This is the root {@link JsonObject}.
 */
public class PythonJsonObject implements JsonObject {

  private List<PythonRelease> releases;

  /**
   * @return the {@link List} of {@link PythonRelease}s.
   */
  public List<PythonRelease> getReleases() {

    return this.releases;
  }
}
