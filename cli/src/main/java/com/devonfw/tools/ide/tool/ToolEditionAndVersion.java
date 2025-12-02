package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Container for a {@link ToolEdition} together with its version.
 */
public class ToolEditionAndVersion {

  private ToolEdition edition;

  private GenericVersionRange version;

  private VersionIdentifier resolvedVersion;

  /**
   * The constructor.
   *
   * @param edition the {@link #getEdition() edition}.
   */
  public ToolEditionAndVersion(ToolEdition edition) {
    super();
    this.edition = edition;
  }

  /**
   * The constructor.
   *
   * @param version the {@link #getVersion() version}.
   */
  public ToolEditionAndVersion(GenericVersionRange version) {
    super();
    if (version != null) {
      setVersion(version);
    }
  }

  /**
   * The constructor.
   *
   * @param edition the {@link #getEdition() edition}.
   * @param version the {@link #getVersion() version}.
   */
  public ToolEditionAndVersion(ToolEdition edition, GenericVersionRange version) {
    this(version);
    assert edition != null;
    assert version != null;
    this.edition = edition;
  }

  /**
   * @return the {@link ToolEdition}.
   */
  public ToolEdition getEdition() {

    return this.edition;
  }

  /**
   * @param edition new value of {@link #getEdition()}.
   */
  public void setEdition(ToolEdition edition) {

    if (this.edition != null) {
      throw new IllegalStateException();
    }
    this.edition = edition;
  }

  /**
   * @return the {@link GenericVersionRange} that is installed or configured.
   */
  public GenericVersionRange getVersion() {

    return this.version;
  }

  /**
   * @param version new value of {@link #getVersion()}.
   */
  public void setVersion(GenericVersionRange version) {
    if (this.version != null) {
      throw new IllegalStateException();
    }
    this.version = version;
    if ((this.resolvedVersion == null) && (version instanceof VersionIdentifier vi)) {
      if (!vi.isPattern()) {
        this.resolvedVersion = vi;
      }
    }
  }

  /**
   * @return the resolved {@link VersionIdentifier}.
   */
  public VersionIdentifier getResolvedVersion() {
    return this.resolvedVersion;
  }

  /**
   * @param resolvedVersion new value of {@link #getResolvedVersion()}.
   */
  public void setResolvedVersion(VersionIdentifier resolvedVersion) {

    if ((resolvedVersion == null) || resolvedVersion.isPattern()) {
      throw new IllegalStateException();
    }
    this.resolvedVersion = resolvedVersion;
  }
}
