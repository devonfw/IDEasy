package com.devonfw.tools.ide.url.model.file.json;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent the Object of the dependencies inside the Json file.
 */
public final class DependencyInfo {

  private String tool;

  private VersionRange versionRange;

  /**
   * @return the dependency name
   */
  public String getTool() {

    return this.tool;
  }

  void setTool(String tool) {

    this.tool = tool;
  }

  /**
   * @return the VersionRange of the dependency
   */
  public VersionRange getVersionRange() {

    return this.versionRange;
  }

  void setVersionRange(VersionRange versionRange) {

    this.versionRange = versionRange;
  }
}
