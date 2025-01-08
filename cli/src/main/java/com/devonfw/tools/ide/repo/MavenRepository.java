package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;
import org.w3c.dom.Document;

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

  private String getBaseUrl(String groupId, String artifactId) {

    if (IDEASY_GROUP_ID.equals(groupId) && IDEASY_ARTIFACT_ID.equals(artifactId) && IdeVersion.get().contains("SNAPSHOT")) {
      return MAVEN_SNAPSHOTS;
    }
    return MAVEN_CENTRAL;
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String groupId, String artifactId, VersionIdentifier version) {

    SystemInfo sys = this.context.getSystemInfo();
    String classifier = sys.getOs() + "-" + sys.getArchitecture();

    String fileName = artifactId + "-" + version + "-" + classifier + ".tar.gz";
    String downloadUrl = getBaseUrl(groupId, artifactId) + "/" + getPath(groupId, artifactId, version.toString(), fileName);
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

        String version = getElementContent((org.w3c.dom.Element) doc.getElementsByTagName("latest").item(0));
        if (version == null || version.isEmpty()) {
          version = getElementContent((org.w3c.dom.Element) doc.getElementsByTagName("release").item(0));
        }

        if (version == null || version.isEmpty()) {
          throw new IllegalStateException("No latest or release version found in metadata");
        }

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

  private String resolveSnapshotVersion(String groupId, String artifactId, String baseVersion) {

    try {
      String metadataUrl = MAVEN_SNAPSHOTS + "/" + getPath(groupId, artifactId, baseVersion, MAVEN_METADATA_XML);
      Path tmpDownloadFile = createTempDownload(MAVEN_METADATA_XML);

      try {
        this.context.getFileAccess().download(metadataUrl, tmpDownloadFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(tmpDownloadFile.toFile());

        String timestamp = getElementContent((org.w3c.dom.Element) doc.getElementsByTagName("timestamp").item(0));
        String buildNumber = getElementContent((org.w3c.dom.Element) doc.getElementsByTagName("buildNumber").item(0));

        // Remove -SNAPSHOT and append timestamp and buildNumber
        String version = baseVersion.replace("-SNAPSHOT", "") + "-" + timestamp + "-" + buildNumber;
        return version;
      } finally {
        this.context.getFileAccess().delete(tmpDownloadFile);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve snapshot version for " + baseVersion, e);
    }
  }

  private String getElementContent(org.w3c.dom.Element element) {

    return element != null ? element.getTextContent() : null;
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

