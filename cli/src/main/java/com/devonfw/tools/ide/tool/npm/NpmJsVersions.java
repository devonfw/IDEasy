package com.devonfw.tools.ide.tool.npm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.devonfw.tools.ide.json.JsonObject;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * {@link JsonObject} for {@link NpmJsVersion}.
 */
public class NpmJsVersions implements JsonObject {

  private Map<String, NpmJsVersion> versions = new TreeMap<>();

  @JsonAnySetter
  public void setDetails(String key, NpmJsVersion val) {

    if (this.versions == null) {
      this.versions = new HashMap<>();
    }
    this.versions.put(key, val);
  }

  /**
   * @return the {@link Map} of {@link NpmJsVersion}s.
   */
  @JsonAnyGetter
  public Map<String, NpmJsVersion> getVersionMap() {

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
    } else if (!(obj instanceof NpmJsVersions)) {
      return false;
    }
    NpmJsVersions other = (NpmJsVersions) obj;
    if (this.versions.size() != other.versions.size()) {
      return false;
    }
    for (String key : this.versions.keySet()) {
      NpmJsVersion myVersion = this.versions.get(key);
      NpmJsVersion otherVersion = other.versions.get(key);
      if (!Objects.equals(myVersion, otherVersion)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {

    return this.versions.toString();
  }
}
