package com.devonfw.tools.ide.maven;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data object for "maven-metadata.xml".
 */
public class MavenMetadata {

  private static final String GROUP_ID = "groupId";

  private static final String ARTIFACT_ID = "artifactId";

  private static final String VERSIONING = "versioning";

  private String groupId;

  private String artifactId;

  private MavenVersioning versioning;

  /**
   * The constructor.
   */
  public MavenMetadata() {

  }

  /**
   * The constructor.
   *
   * @param groupId the {@link #getGroupId() groupId}.
   * @param artifactId the {@link #getArtifactId() artifactId}.
   * @param versioning the {@link #getVersioning() versioning}.
   */
  public MavenMetadata(String groupId, String artifactId, MavenVersioning versioning) {

    this.groupId = groupId;
    this.artifactId = artifactId;
    this.versioning = versioning;
  }

  /**
   * @return the maven groupId (e.g. "com.devonfw.cobigen").
   */
  @JsonProperty(GROUP_ID)
  public String getGroupId() {

    return this.groupId;
  }

  /**
   * @param groupId the new value of {@link #getGroupId()}.
   */
  @JsonProperty(GROUP_ID)
  public void setGroupId(String groupId) {

    this.groupId = groupId;
  }

  /**
   * @return the maven artifactId (e.g. "cobigen-cli").
   */
  @JsonProperty(ARTIFACT_ID)
  public String getArtifactId() {

    return this.artifactId;
  }

  /**
   * @param artifactId the new value of {@link #getArtifactId()}.
   */
  @JsonProperty(ARTIFACT_ID)
  public void setArtifactId(String artifactId) {

    this.artifactId = artifactId;
  }

  /**
   * @return the {@link MavenVersioning}.
   */
  @JsonProperty(VERSIONING)
  public MavenVersioning getVersioning() {

    return this.versioning;
  }

  /**
   * @param versioning the new value of {@link #getVersioning()}.
   */
  @JsonProperty(VERSIONING)
  public void setVersioning(MavenVersioning versioning) {

    this.versioning = versioning;
  }
}
