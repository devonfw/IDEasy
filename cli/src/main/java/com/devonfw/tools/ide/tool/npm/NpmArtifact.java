package com.devonfw.tools.ide.tool.npm;

import java.util.Objects;

import com.devonfw.tools.ide.tool.repository.SoftwareArtifact;

/**
 * Simple type representing a maven artifact.
 */
public final class NpmArtifact extends SoftwareArtifact {

  private final String name;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() name}.
   * @param version the {@link #getVersion() version}.
   */
  public NpmArtifact(String name, String version) {
    super(version);
    this.name = requireNotEmpty(name, "name");
  }

  /**
   * @return the package name (e.g. "corepack" or "@angular/cli").
   */
  public String getName() {
    return this.name;
  }

  /**
   * @param newVersion the new value of {@link #getVersion()}.
   * @return a new {@link NpmArtifact} with the given version.
   */
  public NpmArtifact withVersion(String newVersion) {

    if (this.version.equals(newVersion)) {
      return this;
    }
    return new NpmArtifact(this.name, newVersion);
  }

  @Override
  protected String computeKey() {

    return this.name + '@' + this.version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name, this.version);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof NpmArtifact other) {
      return this.name.equals(other.name) && this.version.equals(other.version);
    }
    return false;
  }

}
