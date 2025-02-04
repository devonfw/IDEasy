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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
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
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList versions = (NodeList) xpath.evaluate("//versions/version", metadata, XPathConstants.NODESET);

      List<VersionIdentifier> versionList = new ArrayList<>();
      for (int i = 0; i < versions.getLength(); i++) {
        versionList.add(VersionIdentifier.of(versions.item(i).getTextContent()));
      }
      versionList.sort(Comparator.reverseOrder());
      return versionList;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to fetch versions from " + source, e);
    }
  }

  private VersionIdentifier resolveSnapshotVersion(String metadataUrl, String baseVersion) {
    Document metadata = fetchXmlMetadata(metadataUrl);
    return resolveSnapshotVersion(metadata, baseVersion);
  }

  VersionIdentifier resolveSnapshotVersion(Document metadata, String baseVersion) {
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      String timestamp = (String) xpath.evaluate("//timestamp", metadata, XPathConstants.STRING);
      String buildNumber = (String) xpath.evaluate("//buildNumber", metadata, XPathConstants.STRING);

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
