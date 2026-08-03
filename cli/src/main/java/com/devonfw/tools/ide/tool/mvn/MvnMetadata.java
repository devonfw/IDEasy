package com.devonfw.tools.ide.tool.mvn;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data object for "maven-metadata.xml".
 */
public class MvnMetadata {

  private static final String GROUP_ID = "groupId";

  private static final String ARTIFACT_ID = "artifactId";

  private static final String VERSIONING = "versioning";

  private String groupId;

  private String artifactId;

  private MvnVersioning versioning;

  /**
   * The constructor.
   */
  public MvnMetadata() {

  }

  /**
   * The constructor.
   *
   * @param groupId the {@link #getGroupId() groupId}.
   * @param artifactId the {@link #getArtifactId() artifactId}.
   * @param versioning the {@link #getVersioning() versioning}.
   */
  public MvnMetadata(String groupId, String artifactId, MvnVersioning versioning) {

    this.groupId = groupId;
    this.artifactId = artifactId;
    this.versioning = versioning;
  }

  /**
   * @return the maven groupId.
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
   * @return the maven artifactId.
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
   * @return the {@link MvnVersioning}.
   */
  @JsonProperty(VERSIONING)
  public MvnVersioning getVersioning() {

    return this.versioning;
  }

  /**
   * @param versioning the new value of {@link #getVersioning()}.
   */
  @JsonProperty(VERSIONING)
  public void setVersioning(MvnVersioning versioning) {

    this.versioning = versioning;
  }
}
