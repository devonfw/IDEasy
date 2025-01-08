package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public class MavenRepository extends AbstractToolRepository {
  private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
  private static final String MAVEN_SNAPSHOTS = "https://s01.oss.sonatype.org/content/repositories/snapshots";
  public static final String MAVEN_METADATA_XML = "maven-metadata.xml";
  private static final String IDEASY_GROUP_ID = "com.devonfw.tools.IDEasy";
  private static final String IDEASY_ARTIFACT_ID = "ide-cli";

  public MavenRepository(IdeContext context) {
    super(context);
  }

  private String getPath(String groupId, String artifactId, String version, String fileName) {
    String basePath = groupId.replace('.', '/') + "/" + artifactId;
    return version != null ? basePath + "/" + version + "/" + fileName : basePath + "/" + fileName;
  }

  private String determineBaseUrl(String groupId, String artifactId, String version) {
    // Special handling for IDEasy
    if (IDEASY_GROUP_ID.equals(groupId) && IDEASY_ARTIFACT_ID.equals(artifactId)) {
      return version != null && version.contains("SNAPSHOT") ? MAVEN_SNAPSHOTS : MAVEN_CENTRAL;
    }
    // Default to Maven Central for all other artifacts
    return MAVEN_CENTRAL;
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

        NodeList snapshotVersions = doc.getElementsByTagName("snapshotVersion");
        SystemInfo sys = this.context.getSystemInfo();
        String classifier = sys.getOs() + "-" + sys.getArchitecture();

        // Find the matching snapshot version for our system
        for (int i = 0; i < snapshotVersions.getLength(); i++) {
          org.w3c.dom.Element snapshotVersion = (org.w3c.dom.Element) snapshotVersions.item(i);
          String snapshotClassifier = getElementContent(snapshotVersion, "classifier");
          String extension = getElementContent(snapshotVersion, "extension");

          if (classifier.equals(snapshotClassifier) && "tar.gz".equals(extension)) {
            return getElementContent(snapshotVersion, "value");
          }
        }
        throw new IllegalStateException("No matching snapshot version found for " + classifier);
      } finally {
        this.context.getFileAccess().delete(tmpDownloadFile);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve snapshot version for " + baseVersion, e);
    }
  }

  private String getElementContent(org.w3c.dom.Element parent, String tagName) {
    NodeList nodes = parent.getElementsByTagName(tagName);
    return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String groupId, String artifactId, VersionIdentifier version) {
    SystemInfo sys = this.context.getSystemInfo();
    String baseUrl = determineBaseUrl(groupId, artifactId, version.toString());
    String classifier = sys.getOs() + "-" + sys.getArchitecture();

    String fileName = artifactId + "-" + version + "-" + classifier + ".tar.gz";
    String downloadUrl = baseUrl + "/" + getPath(groupId, artifactId, version.toString(), fileName);
    return new MavenArtifactMetadata(groupId, artifactId, version, downloadUrl, sys.getOs(), sys.getArchitecture());
  }

  @Override
  public VersionIdentifier resolveVersion(String groupId, String artifactId, GenericVersionRange versionRange) {
    try {
      String baseUrl = determineBaseUrl(groupId, artifactId, null);
      String metadataUrl = baseUrl + "/" + getPath(groupId, artifactId, null, MAVEN_METADATA_XML);

      Path tmpDownloadFile = createTempDownload(MAVEN_METADATA_XML);
      try {
        this.context.getFileAccess().download(metadataUrl, tmpDownloadFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(tmpDownloadFile.toFile());

        String version = getElementContent(doc.getDocumentElement(), "latest");
        if (version == null || version.isEmpty()) {
          version = getElementContent(doc.getDocumentElement(), "release");
        }

        if (version == null || version.isEmpty()) {
          throw new IllegalStateException("No latest or release version found in metadata");
        }

        // Special handling for IDEasy snapshots
        if (IDEASY_GROUP_ID.equals(groupId) && IDEASY_ARTIFACT_ID.equals(artifactId) && version.contains("SNAPSHOT")) {
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

  @Override
  public String getId() {
    return "maven";
  }

  @Override
  public Collection<ToolDependency> findDependencies(String groupId, String artifactId, VersionIdentifier version) {
    return Collections.emptyList();
  }
}