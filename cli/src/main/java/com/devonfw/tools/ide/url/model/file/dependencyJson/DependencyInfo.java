package com.devonfw.tools.ide.url.model.file.dependencyJson;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent the Object of the dependencies inside the Json file.
 */
public final class DependencyInfo {

  private String tool;

  private String versionRange;

  /**
   * @return the dependency name
   */
  public String getTool() {

    return this.tool;
  }

  /**
   * @return the VersionRange of the dependency
   */
  public VersionRange getVersionRange() {

    return VersionRange.of(this.versionRange);
  }

}
