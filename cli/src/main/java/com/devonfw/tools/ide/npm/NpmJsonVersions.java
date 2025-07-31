package com.devonfw.tools.ide.npm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.devonfw.tools.ide.json.JsonObject;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * {@link JsonObject} for {@link NpmJsonVersion}.
 */
public class NpmJsonVersions {

  private Map<String, NpmJsonVersion> versions = new TreeMap<>();

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
  @JsonAnyGetter
  public Map<String, NpmJsonVersion> getVersionMap() {

    return this.versions;
  }

  @Override
  public int hashCode() {

    return this.versions.size();
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    } else if (!(obj instanceof NpmJsonVersions)) {
      return false;
    }
    NpmJsonVersions other = (NpmJsonVersions) obj;
    if (this.versions.size() != other.versions.size()) {
      return false;
    }
    for (String key : this.versions.keySet()) {
      NpmJsonVersion myVersion = this.versions.get(key);
      NpmJsonVersion otherVersion = other.versions.get(key);
      if (!Objects.equals(myVersion, otherVersion)) {
        return false;
      }
    }
    return true;
  }
}
