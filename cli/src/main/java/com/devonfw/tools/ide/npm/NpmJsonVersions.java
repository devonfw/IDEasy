package com.devonfw.tools.ide.npm;

import java.util.HashMap;
import java.util.Map;

import com.devonfw.tools.ide.common.JsonObject;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * {@link JsonObject} for {@link NpmJsonVersion}.
 */
public class NpmJsonVersions {

  private Map<String, NpmJsonVersion> versions;

  @JsonAnySetter
  public void setDetails(String key, NpmJsonVersion val) {

    if (this.versions == null) {
      this.versions = new HashMap<>();
    }
    this.versions.put(key, val);
  }

  /**
   * @return the {@link Map} of {@link NpmJsonVersion}s.
   */
  public Map<String, NpmJsonVersion> getVersionMap() {

    return this.versions;
  }
}
