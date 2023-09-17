package com.devonfw.tools.ide.url.updater.java;

import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.common.JsonObject;

/**
 * {@link JsonObject} for Java versions from adoptium REST API.
 */
public class JavaJsonObject implements JsonObject {
  private final List<JavaJsonVersion> versions = new ArrayList<>();

  /**
   * @return the {@link List} of {@link JavaJsonVersion}s.
   */
  public List<JavaJsonVersion> getVersions() {

    return this.versions;
  }
}
