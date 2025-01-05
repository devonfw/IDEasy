package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.IdeVersion;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public class MavenRepository extends AbstractToolRepository {

  private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  private static final String MAVEN_SNAPSHOTS = "https://s01.oss.sonatype.org/content/repositories/snapshots";

  public static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  public MavenRepository(IdeContext context) {

    super(context);
  }

  private String getArtifactPath(String groupId, String artifactId, String version) {

    return groupId.replace('.', '/') + "/" + artifactId + "/" + version;
  }

  private String getMetadataPath(String groupId, String artifactId) {

    return groupId.replace('.', '/') + "/" + artifactId + "/" + MAVEN_METADATA_XML;
  }

  private String getSnapshotMetadataPath(String groupId, String artifactId, String version) {

    return getArtifactPath(groupId, artifactId, version) + "/" + MAVEN_METADATA_XML;
  }

  private String determineBaseUrl() {

    String currentVersion = IdeVersion.get();
    return currentVersion.contains("SNAPSHOT") ? MAVEN_SNAPSHOTS : MAVEN_CENTRAL;
  }

  private String resolveSnapshotVersion(String groupId, String artifactId, String version) {

    try {
      String baseUrl = MAVEN_SNAPSHOTS; // Always use SNAPSHOT repository for snapshot versions
      String metadataUrl = baseUrl + "/" + getSnapshotMetadataPath(groupId, artifactId, version);

      Path tmpDownloadFile = createTempDownload(MAVEN_METADATA_XML);
      try {
        this.context.getFileAccess().download(metadataUrl, tmpDownloadFile);
        Document doc = XmlMerger.DOCUMENT_BUILDER.parse(tmpDownloadFile.toFile());

        // Get timestamp and buildNumber from snapshot metadata
        String timestamp = doc.getElementsByTagName("timestamp").item(0).getTextContent();
        String buildNumber = doc.getElementsByTagName("buildNumber").item(0).getTextContent();

        // Replace SNAPSHOT with timestamp-buildNumber
        return version.replace("-SNAPSHOT", "-" + timestamp + "-" + buildNumber);
      } finally {
        this.context.getFileAccess().delete(tmpDownloadFile);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve SNAPSHOT version for " + groupId + ":" + artifactId + ":" + version, e);
    }
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String groupId, String artifactId, VersionIdentifier version) {

    SystemInfo sys = this.context.getSystemInfo();
    String baseUrl = determineBaseUrl();
    String versionStr = version.toString();

    // For SNAPSHOT versions, we need to resolve the actual version with timestamp
    if (versionStr.contains("SNAPSHOT")) {
      versionStr = resolveSnapshotVersion(groupId, artifactId, versionStr);
    }

    String downloadUrl = baseUrl + "/" + getArtifactPath(groupId, artifactId, version.toString()) + "/" +
        artifactId + "-" + versionStr + sys.getOs() + sys.getArchitecture() + ".tar.gz";

    return new MavenArtifactMetadata(groupId, artifactId, version, downloadUrl, sys.getOs(), sys.getArchitecture());
  }

  @Override
  public VersionIdentifier resolveVersion(String groupId, String artifactId, GenericVersionRange versionRange) {

    try {
      String baseUrl = determineBaseUrl();
      String metadataUrl = baseUrl + "/" + getMetadataPath(groupId, artifactId);

      Path tmpDownloadFile = createTempDownload(MAVEN_METADATA_XML);
      try {
        this.context.getFileAccess().download(metadataUrl, tmpDownloadFile);
        Document doc = XmlMerger.DOCUMENT_BUILDER.parse(tmpDownloadFile.toFile());

        NodeList versions = doc.getElementsByTagName("version");
        String latestVersion = null;

        for (int i = 0; i < versions.getLength(); i++) {
          String version = versions.item(i).getTextContent();
          VersionIdentifier versionId = VersionIdentifier.of(version);
          if (latestVersion == null || VersionIdentifier.of(latestVersion).compareTo(versionId) < 0) {
            latestVersion = version;
          }
        }

        if (latestVersion == null) {
          throw new IllegalStateException("No versions found in metadata");
        }
        return VersionIdentifier.of(latestVersion);
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
