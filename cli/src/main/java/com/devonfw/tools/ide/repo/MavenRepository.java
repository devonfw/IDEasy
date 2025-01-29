package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
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
import java.lang.Runtime.Version;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AbstractToolRepository} for maven-based artifacts.
 */
public class MavenRepository extends AbstractToolRepository {

  /** Base URL for Maven Central repository */
  public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  /** Base URL for Maven Snapshots repository */
  public static final String MAVEN_SNAPSHOTS = "https://s01.oss.sonatype.org/content/repositories/snapshots";

  private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  private final DocumentBuilder documentBuilder;

  private static final Map<String, String> TOOL_TO_GROUP_ID = Map.of(

      "ideasy", "com.devonfw.tools.IDEasy",
      "cobigen", "com.devonfw.cobigen"
  );

  private String resolveGroupId(String tool) {

    String groupId = TOOL_TO_GROUP_ID.get(tool);
    if (groupId == null) {
      throw new UnsupportedOperationException("Tool '" + tool + "' is not supported by Maven repository.");
    }
    return groupId;
  }


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
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version) {

    return getMetadata(tool, edition, version, null, null);
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version, String classifier, String type) {

    String groupId = resolveGroupId(tool);
    if (type == null) {
      type = MvnArtifact.TYPE_JAR;
    }
    MvnArtifact mvnArtifact = new MvnArtifact(groupId, edition, version.toString(), type, classifier);
    return new MavenArtifactMetadata(mvnArtifact, version);
  }


  @Override
  public VersionIdentifier resolveVersion(String groupId, String artifactId, GenericVersionRange version) {

    String baseUrl;
    if (version == VersionIdentifier.LATEST_UNSTABLE) {
      baseUrl = MAVEN_SNAPSHOTS;
    } else {
      baseUrl = MAVEN_CENTRAL;
    }
    String metadataUrl = baseUrl + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + MAVEN_METADATA_XML;
    List<VersionIdentifier> versions = fetchVersions(metadataUrl);
    // beta versions of maven central are also considered unstable and "*" doesn't match them
    VersionIdentifier resolvedVersion = this.context.getUrls().resolveVersionPattern(version, versions);
    if (resolvedVersion.toString().endsWith("-SNAPSHOT")) {
      String snapshotMetadataUrl = baseUrl + "/" + groupId.replace('.', '/') + "/" +
          artifactId + "/" + resolvedVersion + "/" + MAVEN_METADATA_XML;
      return resolveSnapshotVersion(snapshotMetadataUrl, resolvedVersion.toString());
    }
    return resolvedVersion;
  }

  private List<VersionIdentifier> fetchVersions(String metadataUrl) {
    try {
      Document doc = fetchXmlMetadata(metadataUrl);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList versions = (NodeList) xpath.evaluate("//versions/version", doc, XPathConstants.NODESET);

      List<VersionIdentifier> versionList = new ArrayList<>();
      for (int i = 0; i < versions.getLength(); i++) {
        versionList.add(VersionIdentifier.of(versions.item(i).getTextContent()));
      }
      Collections.sort(versionList, Comparator.reverseOrder());
      return versionList;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to fetch versions from " + metadataUrl, e);
    }
  }

  private VersionIdentifier resolveSnapshotVersion(String metadataUrl, String baseVersion) {
    try {
      Document doc = fetchXmlMetadata(metadataUrl);
      XPath xpath = XPathFactory.newInstance().newXPath();
      String timestamp = (String) xpath.evaluate("//timestamp", doc, XPathConstants.STRING);
      String buildNumber = (String) xpath.evaluate("//buildNumber", doc, XPathConstants.STRING);

      if (timestamp.isEmpty() || buildNumber.isEmpty()) {
        throw new IllegalStateException("Missing timestamp or buildNumber in snapshot metadata");
      }

      String version = baseVersion.replace("-SNAPSHOT", "-" + timestamp + "-" + buildNumber);
      return VersionIdentifier.of(version);
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
