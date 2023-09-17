package com.devonfw.tools.ide.maven;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data object for {@link MavenMetadata#getVersioning() versioning} of {@link MavenMetadata}.
 */
public class MavenVersioning {

  private String latest;

  private String release;

  private List<String> versions;

  private String lastUpdated;

  /**
   * The constructor.
   */
  public MavenVersioning() {

    super();
  }

  /**
   *
   * The constructor.
   *
   * @param latest the {@link #getLatest() latest} version.
   * @param release the {@link #getRelease() release} version.
   * @param versions the {@link #getVersions() versions}.
   * @param lastUpdated the {@link #getLastUpdated() last updated} timestamp.
   */
  public MavenVersioning(String latest, String release, List<String> versions, String lastUpdated) {

    this.latest = latest;
    this.release = release;
    this.versions = versions;
    this.lastUpdated = lastUpdated;
  }

  /**
   * @return the latest version.
   */
  @JsonProperty("latest")
  public String getLatest() {

    return this.latest;
  }

  /**
   * @param latest the new value of {@link #getLatest()}.
   */
  @JsonProperty("latest")
  public void setLatest(String latest) {

    this.latest = latest;
  }

  /**
   * @return the most recent release version.
   */
  @JsonProperty("release")
  public String getRelease() {

    return this.release;
  }

  /**
   * @param release the new value of {@link #getRelease()}.
   */
  @JsonProperty("release")
  public void setRelease(String release) {

    this.release = release;
  }

  /**
   * @return the {@link List} with all available versions.
   */
  @JsonProperty("versions")
  public List<String> getVersions() {

    return this.versions;
  }

  /**
   * @param versions the new value of {@link #getVersions()}.
   */
  @JsonProperty("versions")
  public void setVersions(List<String> versions) {

    this.versions = versions;
  }

  /**
   * @return the timestamp of the last update.
   */
  @JsonProperty("lastUpdated")
  public String getLastUpdated() {

    return this.lastUpdated;
  }

  /**
   * @param lastUpdated the new value of {@link #getLastUpdated()}.
   */
  @JsonProperty("lastUpdated")
  public void setLastUpdated(String lastUpdated) {

    this.lastUpdated = lastUpdated;
  }
}