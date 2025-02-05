package com.devonfw.tools.ide.tool.repository;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link AbstractToolRepository} for maven-based artifacts.
 */
public class MavenRepository extends AbstractToolRepository {

  /** Base URL for Maven Central repository */
  public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  /** Base URL for Maven Snapshots repository */
  public static final String MAVEN_SNAPSHOTS = "https://s01.oss.sonatype.org/content/repositories/snapshots";

  private final DocumentBuilder documentBuilder;

  private static final Map<String, MvnArtifact> TOOL_MAP = Map.of(
      "ideasy", MvnArtifact.ofIdeasyCli("*", "tar.gz", "${os}-${arch}"),
      "gcviewer", new MvnArtifact("com.github.chewiebug", "gcviewer", "*")
  );

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

  private MavenArtifactMetadata resolveArtifact(String tool, String edition, VersionIdentifier version) {

    String key = tool;
    if (!tool.equals(edition)) {
      key = tool + ":" + edition;
    }
    MvnArtifact artifact = TOOL_MAP.get(key);
    if (artifact == null) {
      throw new UnsupportedOperationException("Tool '" + key + "' is not supported by Maven repository.");
    }
    OperatingSystem os = null;
    SystemArchitecture arch = null;
    String classifier = artifact.getClassifier();
    if (!classifier.isEmpty()) {
      String resolvedClassifier;
      os = this.context.getSystemInfo().getOs();
      resolvedClassifier = classifier.replace("${os}", os.toString());
      if (resolvedClassifier.equals(classifier)) {
        os = null;
      } else {
        classifier = resolvedClassifier;
      }
      arch = this.context.getSystemInfo().getArchitecture();
      resolvedClassifier = classifier.replace("${arch}", arch.toString());
      if (resolvedClassifier.equals(classifier)) {
        arch = null;
      }
      artifact = artifact.withClassifier(resolvedClassifier);
    }
    if (version != null) {
      artifact = artifact.withVersion(version.toString());
    }
    return new MavenArtifactMetadata(artifact, os, arch);
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version) {

    return resolveArtifact(tool, edition, version);
  }


  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version) {

    MavenArtifactMetadata artifactMetadata = resolveArtifact(tool, edition, null);
    MvnArtifact artifact = artifactMetadata.getMvnArtifact();
    return resolveVersion(artifact, version);
  }

  /**
   * @param artifact the {@link MvnArtifact} to resolve. Should not have a version set.
   * @param version the {@link GenericVersionRange} to resolve.
   * @return the resolved {@link VersionIdentifier}.
   */
  public VersionIdentifier resolveVersion(MvnArtifact artifact, GenericVersionRange version) {

    artifact = artifact.withMavenMetadata();
    String versionString = version.toString();
    if (versionString.startsWith("*")) {
      artifact = artifact.withVersion(versionString);
    }
    List<VersionIdentifier> versions = fetchVersions(artifact.getDownloadUrl());
    VersionIdentifier resolvedVersion = this.context.getUrls().resolveVersionPattern(version, versions);
    versionString = resolvedVersion.toString();
    if (versionString.endsWith("-SNAPSHOT")) {
      artifact = artifact.withVersion(versionString);
      return resolveSnapshotVersion(artifact.getDownloadUrl(), versionString);
    }
    return resolvedVersion;
  }

  private List<VersionIdentifier> fetchVersions(String metadataUrl) {

    Document metadata = fetchXmlMetadata(metadataUrl);
    return fetchVersions(metadata, metadataUrl);
  }

  List<VersionIdentifier> fetchVersions(Document metadata, String source) {
    Element versioning = getFirstChildElement(metadata.getDocumentElement(), "versioning", source);
    Element versions = getFirstChildElement(versioning, "versions", source);
    NodeList versionsChildren = versions.getElementsByTagName("version");
    int length = versionsChildren.getLength();
    List<VersionIdentifier> versionList = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      versionList.add(VersionIdentifier.of(versionsChildren.item(i).getTextContent()));
    }
    versionList.sort(Comparator.reverseOrder());
    return versionList;
  }

  private VersionIdentifier resolveSnapshotVersion(String metadataUrl, String baseVersion) {
    Document metadata = fetchXmlMetadata(metadataUrl);
    return resolveSnapshotVersion(metadata, baseVersion, metadataUrl);
  }

  VersionIdentifier resolveSnapshotVersion(Document metadata, String baseVersion, String source) {
    Element versioning = getFirstChildElement(metadata.getDocumentElement(), "versioning", source);
    Element snapshot = getFirstChildElement(versioning, "snapshot", source);
    String timestamp = getFirstChildElement(snapshot, "timestamp", source).getTextContent();
    String buildNumber = getFirstChildElement(snapshot, "buildNumber", source).getTextContent();
    String version = baseVersion.replace("-SNAPSHOT", "-" + timestamp + "-" + buildNumber);
    return VersionIdentifier.of(version);
  }

  private Element getFirstChildElement(Element element, String tag, Object source) {

    NodeList children = element.getChildNodes();
    int length = children.getLength();
    for (int i = 0; i < length; i++) {
      Node node = children.item(i);
      if (node instanceof Element child) {
        if (child.getTagName().equals(tag)) {
          return child;
        }
      }
    }
    throw new IllegalStateException("Failed to resolve snapshot version - element " + tag + " not found in " + source);
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
