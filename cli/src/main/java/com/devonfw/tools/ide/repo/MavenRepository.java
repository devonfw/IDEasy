package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of {@link AbstractToolRepository} for maven-based artifacts.
 */
public class MavenRepository extends AbstractToolRepository {

  private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  private static final String MAVEN_SNAPSHOTS = "https://s01.oss.sonatype.org/content/repositories/snapshots";

  private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  private static final String DEFAULT_EXTENSION = ".jar";

  private final DocumentBuilder documentBuilder;

  private static final String SNAPSHOT_VERSION_PATTERN = ".*-\\d{8}\\.\\d{6}-\\d+";

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public MavenRepository(IdeContext context) {

    super(context);
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      this.documentBuilder = factory.newDocumentBuilder();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create XML document builder", e);
    }
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String groupId, String artifactId, VersionIdentifier version) {

    return getMetadata(groupId, artifactId, version, null, null);
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String groupId, String artifactId, VersionIdentifier version, String classifier, String extension) {

    if (extension == null || extension.isEmpty()) {
      extension = DEFAULT_EXTENSION;
    }

    String versionStr = version.toString();
    boolean isSnapshot = versionStr.matches(SNAPSHOT_VERSION_PATTERN);
    String pathVersion = isSnapshot ?
        versionStr.replaceAll("-\\d{8}\\.\\d{6}-\\d+", "-SNAPSHOT") :
        versionStr;

    StringBuilder fileNameBuilder = new StringBuilder()
        .append(artifactId)
        .append("-")
        .append(versionStr);

    if (classifier != null && !classifier.isEmpty()) {
      fileNameBuilder.append("-").append(classifier);
    }
    fileNameBuilder.append(extension);

    String fileName = fileNameBuilder.toString();
    String baseUrl = isSnapshot ? MAVEN_SNAPSHOTS : MAVEN_CENTRAL;
    String downloadUrl = baseUrl + "/" + getPath(groupId, artifactId, pathVersion, fileName);

    return new MavenArtifactMetadata(groupId, artifactId, version, downloadUrl, null, null);
  }


  @Override
  public VersionIdentifier resolveVersion(String groupId, String artifactId, GenericVersionRange versionRange) {

    try {
      String baseUrl = versionRange.toString().endsWith("-SNAPSHOT") ? MAVEN_SNAPSHOTS : MAVEN_CENTRAL;
      String metadataUrl = baseUrl + "/" + getPath(groupId, artifactId, null, MAVEN_METADATA_XML);
      Document doc = fetchXmlMetadata(metadataUrl);

      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList versions = (NodeList) xpath.evaluate("//versions/version", doc, XPathConstants.NODESET);

      if (versions.getLength() == 0) {
        throw new IllegalStateException("No versions found in metadata of " + artifactId);
      }

      VersionIdentifier latestVersion = null;
      for (int i = 0; i < versions.getLength(); i++) {
        VersionIdentifier version = VersionIdentifier.of(versions.item(i).getTextContent());
        if (latestVersion == null || latestVersion.compareVersion(version).isLess()) {
          latestVersion = version;
        }
      }

      if (latestVersion.toString().endsWith("-SNAPSHOT")) {
        return VersionIdentifier.of(resolveSnapshotVersion(groupId, artifactId, latestVersion.toString()));
      }

      return latestVersion;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve version for " + groupId + ":" + artifactId, e);
    }
  }

  private String resolveSnapshotVersion(String groupId, String artifactId, String baseVersion) {

    try {
      String metadataUrl = MAVEN_SNAPSHOTS + "/" + getPath(groupId, artifactId, baseVersion, MAVEN_METADATA_XML);
      Document doc = fetchXmlMetadata(metadataUrl);

      XPath xpath = XPathFactory.newInstance().newXPath();
      String timestamp = (String) xpath.evaluate("//timestamp", doc, XPathConstants.STRING);
      String buildNumber = (String) xpath.evaluate("//buildNumber", doc, XPathConstants.STRING);

      if (timestamp.isEmpty() || buildNumber.isEmpty()) {
        throw new IllegalStateException("Missing timestamp or buildNumber in snapshot metadata");
      }

      return baseVersion.replace("-SNAPSHOT", "") + "-" + timestamp + "-" + buildNumber;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve snapshot version for " + baseVersion, e);
    }
  }

  private Document fetchXmlMetadata(String url) {

    try {
      URL xmlUrl = new URL(url);
      try (InputStream is = xmlUrl.openStream()) {
        return documentBuilder.parse(is);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to fetch XML metadata from " + url, e);
    }
  }

  private String getPath(String groupId, String artifactId, String version, String fileName) {

    String basePath = groupId.replace('.', '/') + "/" + artifactId;
    return version != null ? basePath + "/" + version + "/" + fileName : basePath + "/" + fileName;
  }

  @Override
  public String getId() {

    return "maven";
  }

  @Override
  public Collection<ToolDependency> findDependencies(String groupId, String artifactId, VersionIdentifier version) {

    // We could read POM here and find dependencies but we do not want to reimplement maven here.
    // For our use-case we only download bundled packages from maven central so we do KISS for now.
    return Collections.emptyList();
  }
}
