package com.devonfw.tools.ide.npm;

import java.util.List;

import com.devonfw.tools.ide.common.JsonObject;

/**
 * {@link JsonObject} for Npm.
 */
public class NpmJsonObject implements JsonObject {

  private NpmJsonVersions versions;

  /**
   * @return the {@link List} of {@link NpmJsonVersion}s.
   */
  public NpmJsonVersions getVersions() {

    return this.versions;
  }
}
