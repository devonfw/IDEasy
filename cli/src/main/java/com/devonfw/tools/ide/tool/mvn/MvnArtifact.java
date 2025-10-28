package com.devonfw.tools.ide.tool.mvn;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.tool.repository.MvnRepository;
import com.devonfw.tools.ide.tool.repository.SoftwareArtifact;

/**
 * Simple type representing a maven artifact.
 */
public class MvnArtifact extends SoftwareArtifact {

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

  /** {@link #getFilename() Filename} for artifact metadata with version information. */
  public static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile("-\\d{8}\\.\\d{6}-\\d+");

  private final String groupId;

  private final String artifactId;

  private final String classifier;

  private final String type;

  private final String filename;

  private String path;

  private String downloadUrl;

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
    this(groupId, artifactId, version, type, classifier, null);
  }

  MvnArtifact(String groupId, String artifactId, String version, String type, String classifier, String filename) {
    super(version);
    this.groupId = requireNotEmpty(groupId, "groupId");
    this.artifactId = requireNotEmpty(artifactId, "artifactId");
    this.classifier = notNull(classifier);
    this.type = requireNotEmpty(type, "type");
    this.filename = filename;
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
   * @param newVersion the new value of {@link #getVersion()}.
   * @return a new {@link MvnArtifact} with the given version.
   */
  public MvnArtifact withVersion(String newVersion) {

    if (this.version.equals(newVersion)) {
      return this;
    }
    return new MvnArtifact(this.groupId, this.artifactId, newVersion, this.type, this.classifier, this.filename);
  }

  /**
   * @return the classifier. Will be the empty {@link String} for no classifier.
   */
  public String getClassifier() {
    return this.classifier;
  }

  /**
   * @param newClassifier the new value of {@link #getClassifier()}.
   * @return a new {@link MvnArtifact} with the given classifier.
   */
  public MvnArtifact withClassifier(String newClassifier) {

    if (this.classifier.equals(newClassifier)) {
      return this;
    }
    return new MvnArtifact(this.groupId, this.artifactId, this.version, this.type, newClassifier, this.filename);
  }

  /**
   * @return the type (e.g. #TYPE_JAR}
   */
  public String getType() {
    return type;
  }

  /**
   * @param newType the new value of {@link #getType()}.
   * @return a new {@link MvnArtifact} with the given type.
   */
  public MvnArtifact withType(String newType) {

    if (this.type.equals(newType)) {
      return this;
    }
    return new MvnArtifact(this.groupId, this.artifactId, this.version, newType, this.classifier, this.filename);
  }

  /**
   * @return the filename of the artifact.
   */
  public String getFilename() {

    if (this.filename == null) {
      String infix = "";
      if (!this.classifier.isEmpty()) {
        infix = "-" + this.classifier;
      }
      return this.artifactId + "-" + this.version + infix + "." + this.type;
    }
    return this.filename;
  }

  /**
   * @param newFilename the new value of {@link #getFilename()}.
   * @return a new {@link MvnArtifact} with the given filename.
   */
  public MvnArtifact withFilename(String newFilename) {

    if (Objects.equals(this.filename, newFilename)) {
      return this;
    }
    return new MvnArtifact(this.groupId, this.artifactId, this.version, this.type, this.classifier, newFilename);
  }

  /**
   * @return a new {@link MvnArtifact} for {@link #MAVEN_METADATA_XML}.
   */
  public MvnArtifact withMavenMetadata() {

    return withType("xml").withFilename(MAVEN_METADATA_XML);
  }

  /**
   * @return {@code true} if this artifact represents {@link #MAVEN_METADATA_XML}.
   * @see #withMavenMetadata()
   */
  public boolean isMavenMetadata() {
    return MAVEN_METADATA_XML.equals(this.filename);
  }

  /**
   * @return the {@link String} with the path to the specified artifact relative to the maven repository base path or URL. For snapshots, includes the
   *     timestamped version in the artifact filename.
   */
  public String getPath() {
    if (this.path == null) {
      StringBuilder sb = new StringBuilder();
      // Common path start: groupId/artifactId/version
      sb.append(this.groupId.replace('.', '/')).append('/')
          .append(this.artifactId).append('/');

      if (!this.version.startsWith("*")) {
        sb.append(getBaseVersion()).append('/');
      }
      sb.append(getFilename());
      this.path = sb.toString();
    }
    return this.path;
  }

  @Override
  protected String computeKey() {

    int capacity = this.groupId.length() + this.artifactId.length() + this.version.length() + type.length() + classifier.length() + 4;
    StringBuilder sb = new StringBuilder(capacity);
    sb.append(this.groupId).append(':').append(this.artifactId).append(':').append(this.version).append(':').append(this.type);
    if (!this.classifier.isEmpty()) {
      sb.append(':').append(this.classifier);
    }
    String key = sb.toString();
    assert (key.length() <= capacity);
    return key;
  }

  /**
   * Checks if the current artifact version is a snapshot version.
   *
   * @return true if this is a snapshot version, false otherwise
   */
  public boolean isSnapshot() {
    return this.version.endsWith("-SNAPSHOT") || SNAPSHOT_VERSION_PATTERN.matcher(this.version).find();
  }

  /**
   * Gets the base version without snapshot timestamp. For snapshot versions like "2024.04.001-beta-20240419.123456-1", returns "2024.04.001-beta-SNAPSHOT". For
   * release versions, returns the version as is.
   *
   * @return the base version
   */
  public String getBaseVersion() {
    Matcher matcher = SNAPSHOT_VERSION_PATTERN.matcher(this.version);
    if (matcher.find()) {
      return matcher.replaceAll("-SNAPSHOT");
    }
    return this.version;
  }

  /**
   * @return the download URL to download the artifact from the maven repository.
   */
  public String getDownloadUrl() {
    if (this.downloadUrl == null) {
      String baseUrl = getMvnBaseUrl();
      this.downloadUrl = baseUrl + "/" + getPath();
    }
    return this.downloadUrl;
  }

  public String getMvnBaseUrl() {
    return isSnapshot() ? MvnRepository.MAVEN_SNAPSHOTS : MvnRepository.MAVEN_CENTRAL;
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
          && this.classifier.equals(other.classifier) && this.type.equals(other.type) && Objects.equals(this.filename, other.filename);
    }
    return false;
  }

  private static String notNull(String value) {

    if (value == null) {
      return "";
    }
    return value;
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
