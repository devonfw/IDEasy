package com.devonfw.tools.ide.npm;

import com.devonfw.tools.ide.common.JsonObject;

import java.util.List;

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