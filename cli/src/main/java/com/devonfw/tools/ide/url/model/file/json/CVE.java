package com.devonfw.tools.ide.url.model.file.json;

import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent a CVE of a tool (inside a "security.json" file).
 */
public record CVE(String id, double severity, List<VersionRange> versions) {


  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    CVE other = (CVE) obj;
    return Objects.equals(this.id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id);
  }

}
