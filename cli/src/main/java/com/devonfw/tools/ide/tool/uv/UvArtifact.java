package com.devonfw.tools.ide.tool.uv;

import java.util.Objects;

import com.devonfw.tools.ide.tool.repository.SoftwareArtifact;

/**
 * Simple type representing a uv tool artifact resolved from PyPI.
 */
public final class UvArtifact extends SoftwareArtifact {

  private final String name;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() name}.
   * @param version the {@link #getVersion() version}.
   */
  public UvArtifact(String name, String version) {
    super(version);
    this.name = requireNotEmpty(name, "name");
  }

  /**
   * @return the package name in PyPi.
   */
  public String getName() {
    return this.name;
  }

  @Override
  protected String computeKey() {
    return this.name + "@" + this.version;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof UvArtifact other) {
      return this.name.equals(other.name) && this.version.equals(other.version);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name, this.version);
  }
}
