package com.devonfw.tools.ide.tool.mvn;

import java.util.Objects;

/**
 * Simple type representing a maven artifact.
 */
public final class MvnArtifact {

  /** {@link #getGroupId() Group ID} of IDEasy. */
  public static final String GROUP_ID_IDEASY = "com.devonfw.tools.IDEasy";

  /** {@link #getArtifactId() Artifact ID} of IDEasy command line interface. */
  public static final String ARTIFACT_ID_IDEASY_CLI = "ide-cli";

  /** {@link #getClassifier() Classifier} of source code. */
  public static final String CLASSIFER_SOURCES = "sources";

  /** {@link #getType() Type} of JAR file. */
  public static final String TYPE_JAR = "jar";

  /** {@link #getType() Type} of POM XML file. */
  public static final String TYPE_POM = "pom";

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String classifier;

  private final String type;

  private String path;

  private String key;

  /**
   * The constructor.
   *
   * @param groupId the {@link #getGroupId() group ID}.
   * @param artifactId the {@link #getArtifactId() artifact ID}.
   * @param version the {@link #getVersion() version}.
   */
  public MvnArtifact(String groupId, String artifactId, String version) {
    this(groupId, artifactId, version, TYPE_JAR);
  }

  /**
   * The constructor.
   *
   * @param groupId the {@link #getGroupId() group ID}.
   * @param artifactId the {@link #getArtifactId() artifact ID}.
   * @param version the {@link #getVersion() version}.
   * @param type the {@link #getType() type}.
   */
  public MvnArtifact(String groupId, String artifactId, String version, String type) {
    this(groupId, artifactId, version, type, "");
  }

  /**
   * The constructor.
   *
   * @param groupId the {@link #getGroupId() group ID}.
   * @param artifactId the {@link #getArtifactId() artifact ID}.
   * @param version the {@link #getVersion() version}.
   * @param type the {@link #getType() type}.
   * @param classifier the {@link #getClassifier() classifier}.
   */
  public MvnArtifact(String groupId, String artifactId, String version, String type, String classifier) {
    super();
    this.groupId = requireNotEmpty(groupId, "groupId");
    this.artifactId = requireNotEmpty(artifactId, "artifactId");
    this.version = requireNotEmpty(version, "version");
    this.classifier = notNull(classifier);
    this.type = requireNotEmpty(type, "type");
  }

  /**
   * @return the group ID (e.g. {@link #GROUP_ID_IDEASY}).
   */
  public String getGroupId() {
    return this.groupId;
  }

  /**
   * @return the artifact ID (e.g. {@link #ARTIFACT_ID_IDEASY_CLI}).
   */
  public String getArtifactId() {
    return this.artifactId;
  }

  /**
   * @return the version.
   * @see com.devonfw.tools.ide.version.VersionIdentifier
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * @return the classifier. Will be the empty {@link String} for no classifier.
   */
  public String getClassifier() {
    return this.classifier;
  }

  /**
   * @return the type (e.g. #TYPE_JAR}
   */
  public String getType() {
    return type;
  }

  /**
   * @return the {@link String} with the path to the specified artifact relative to the maven repository base path or URL.
   */
  public String getPath() {
    if (this.path == null) {
      int capacity = this.groupId.length() + 2 * this.artifactId.length() + 2 * this.version.length() + type.length() + classifier.length() + 6;
      StringBuilder sb = new StringBuilder(capacity);
      sb.append(this.groupId.replace('.', '/')).append('/')
          .append(this.artifactId).append('/')
          .append(this.version).append('/')
          .append(this.artifactId).append('-').append(this.version);
      if (!this.classifier.isEmpty()) {
        sb.append('-').append(this.classifier);
      }
      sb.append('.').append(this.type);
      this.path = sb.toString();
      assert (this.path.length() <= capacity);
    }
    return this.path;
  }

  /**
   * @return the artifact key as unique identifier.
   */
  String getKey() {
    if (this.key == null) {
      int capacity = this.groupId.length() + this.artifactId.length() + this.version.length() + type.length() + classifier.length() + 4;
      StringBuilder sb = new StringBuilder(capacity);
      sb.append(this.groupId).append(':').append(this.artifactId).append(':').append(this.version).append(':').append(this.type);
      if (!this.classifier.isEmpty()) {
        sb.append(':').append(this.classifier);
      }
      this.key = sb.toString();
      assert (this.key.length() <= capacity);
    }
    return this.key;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.groupId, this.artifactId, this.version);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof MvnArtifact other) {
      return this.groupId.equals(other.groupId) && this.artifactId.equals(other.artifactId) && this.version.equals(other.version)
          && this.classifier.equals(other.classifier) && this.type.equals(other.type);
    }
    return false;
  }

  @Override
  public String toString() {
    return getKey();
  }

  private static String notNull(String value) {

    if (value == null) {
      return "";
    }
    return value;
  }

  private static String requireNotEmpty(String value, String propertyName) {

    if (isEmpty(value)) {
      throw new IllegalArgumentException("Maven artifact property " + propertyName + " must not be empty");
    }
    return value;
  }

  private static boolean isEmpty(String value) {

    return ((value == null) || value.isEmpty());
  }

  /**
   * @param artifactId the {@link #getArtifactId() artifact ID}.
   * @param version the {@link #getVersion() version}.
   * @param type the {@link #getType() type}.
   * @param classifier the {@link #getClassifier() classifier}.
   * @return the IDEasy {@link MvnArtifact}.
   */
  public static MvnArtifact ofIdeasy(String artifactId, String version, String type, String classifier) {

    return new MvnArtifact(GROUP_ID_IDEASY, artifactId, version, type, classifier);
  }

  /**
   * @param version the {@link #getVersion() version}.
   * @param type the {@link #getType() type}.
   * @param classifier the {@link #getClassifier() classifier}.
   * @return the IDEasy {@link MvnArtifact}.
   */
  public static MvnArtifact ofIdeasyCli(String version, String type, String classifier) {

    return ofIdeasy(ARTIFACT_ID_IDEASY_CLI, version, type, classifier);
  }
}
