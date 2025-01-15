package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Implementation of {@link AbstractToolRepository} for maven-based artifacts.
 */
public class MavenRepository extends AbstractToolRepository {

  private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  private static final String MAVEN_SNAPSHOTS = "https://s01.oss.sonatype.org/content/repositories/snapshots";

  private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  /** Group id of IDEasy. */
  public static final String IDEASY_GROUP_ID = "com.devonfw.tools.IDEasy";

  /** Artifact Id of IDEasy. */
  public static final String IDEASY_ARTIFACT_ID = "ide-cli";

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public MavenRepository(IdeContext context) {

    super(context);
  }

  private String getPath(String groupId, String artifactId, String version, String fileName) {

    String basePath = groupId.replace('.', '/') + "/" + artifactId;
    return version != null ? basePath + "/" + version + "/" + fileName : basePath + "/" + fileName;
  }

  private String getBaseUrl(String groupId, String artifactId) {

    if (isIdeasySnapshot(groupId, artifactId, IdeVersion.get())) {
      return MAVEN_SNAPSHOTS;
    }
    return MAVEN_CENTRAL;
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String groupId, String artifactId, VersionIdentifier version) {

    SystemInfo sys = this.context.getSystemInfo();
    String classifier = sys.getOs() + "-" + sys.getArchitecture();

    String pathVersion = version.toString();
    // If it's a resolved snapshot version (contains timestamp), convert back to -SNAPSHOT format for the path
    if (pathVersion.matches(".*-\\d{8}\\.\\d{6}-\\d+")) {
      pathVersion = pathVersion.replaceAll("-\\d{8}\\.\\d{6}-\\d+", "-SNAPSHOT");
    }

    // hardcoding the file extension seems wrong
    String fileName = artifactId + "-" + version + "-" + classifier + ".tar.gz";
    String downloadUrl = getBaseUrl(groupId, artifactId) + "/" +
        getPath(groupId, artifactId, pathVersion, fileName);
    return new MavenArtifactMetadata(groupId, artifactId, version, downloadUrl, sys.getOs(), sys.getArchitecture());
  }

  @Override
  public VersionIdentifier resolveVersion(String groupId, String artifactId, GenericVersionRange versionRange) {

    try {
      String metadataUrl = getBaseUrl(groupId, artifactId) + "/" + getPath(groupId, artifactId, null, MAVEN_METADATA_XML);
      Path tmpDownloadFile = createTempDownload(MAVEN_METADATA_XML);

      try {
        this.context.getFileAccess().download(metadataUrl, tmpDownloadFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(tmpDownloadFile.toFile());

        String version = Optional.ofNullable(doc.getElementsByTagName("latest").item(0))
            .map(Element.class::cast)
            .map(Element::getTextContent)
            .orElseGet(() -> Optional.ofNullable(doc.getElementsByTagName("release").item(0))
                .map(Element.class::cast)
                .map(Element::getTextContent)
                .orElseThrow(() -> new IllegalStateException("No latest or release version found in metadata")));

        if (isIdeasySnapshot(groupId, artifactId, version)) {
          version = resolveSnapshotVersion(groupId, artifactId, version);
        }

        return VersionIdentifier.of(version);
      } finally {
        this.context.getFileAccess().delete(tmpDownloadFile);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve version for " + groupId + ":" + artifactId, e);
    }
  }

  private String resolveSnapshotVersion(String groupId, String artifactId, String baseVersion) {

    try {
      String metadataUrl = MAVEN_SNAPSHOTS + "/" + getPath(groupId, artifactId, baseVersion, MAVEN_METADATA_XML);
      Path tmpDownloadFile = createTempDownload(MAVEN_METADATA_XML);

      try {
        this.context.getFileAccess().download(metadataUrl, tmpDownloadFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(tmpDownloadFile.toFile());

        String timestamp = Optional.ofNullable(doc.getElementsByTagName("timestamp").item(0))
            .map(Element.class::cast)
            .map(Element::getTextContent)
            .orElseThrow(() -> new IllegalStateException("No timestamp found in snapshot metadata"));

        String buildNumber = Optional.ofNullable(doc.getElementsByTagName("buildNumber").item(0))
            .map(Element.class::cast)
            .map(Element::getTextContent)
            .orElseThrow(() -> new IllegalStateException("No buildNumber found in snapshot metadata"));

        return baseVersion.replace("-SNAPSHOT", "") + "-" + timestamp + "-" + buildNumber;
      } finally {
        this.context.getFileAccess().delete(tmpDownloadFile);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve snapshot version for " + baseVersion, e);
    }
  }

  private boolean isIdeasySnapshot(String groupId, String artifactId, String version) {

    return IDEASY_GROUP_ID.equals(groupId) && IDEASY_ARTIFACT_ID.equals(artifactId) && version != null && version.contains("SNAPSHOT");
  }

  @Override
  public String getId() {

    return "maven";
  }

  @Override
  public Collection<ToolDependency> findDependencies(String groupId, String artifactId, VersionIdentifier version) {

    return Collections.emptyList();
  }
}
