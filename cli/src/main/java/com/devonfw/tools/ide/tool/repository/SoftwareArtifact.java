package com.devonfw.tools.ide.tool.repository;

public abstract class SoftwareArtifact {

  protected final String version;

  private String key;

  /**
   * The constructor.
   *
   * @param version the {@link #getVersion() version}.
   */
  public SoftwareArtifact(String version) {
    super();
    this.version = requireNotEmpty(version, "version");
  }

  /**
   * @return the version.
   * @see com.devonfw.tools.ide.version.VersionIdentifier
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * @return the artifact key as unique identifier.
   */
  public String getKey() {

    if (this.key == null) {
      this.key = computeKey();
    }
    return this.key;
  }

  protected abstract String computeKey();

  @Override
  public String toString() {
    return getKey();
  }

  protected static String requireNotEmpty(String value, String propertyName) {

    if (isEmpty(value)) {
      throw new IllegalArgumentException("Artifact property " + propertyName + " must not be empty.");
    }
    return value;
  }

  private static boolean isEmpty(String value) {

    return ((value == null) || value.isEmpty());
  }

}
