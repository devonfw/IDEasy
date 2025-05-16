package com.devonfw.tools.ide.tool.repository;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.UrlGenericChecksum;
import com.devonfw.tools.ide.url.model.file.UrlGenericChecksumType;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link AbstractToolRepository} for maven-based artifacts.
 */
public final class MavenRepository extends AbstractToolRepository {

  /** Base URL for Maven Central repository */
  public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  /** Base URL for Maven Snapshots repository */
  public static final String MAVEN_SNAPSHOTS = "https://s01.oss.sonatype.org/content/repositories/snapshots";

  /** The {@link #getId() repository ID}. */
  public static final String ID = "maven";

  private static final Duration METADATA_CACHE_DURATION_RELEASE = Duration.ofHours(1);

  private static final Duration METADATA_CACHE_DURATION_SNAPSHOT = Duration.ofMinutes(5);

  private final Path localMavenRepository;

  private final DocumentBuilder documentBuilder;

  private static final Map<String, MvnArtifact> TOOL_MAP = Map.of(
      "ideasy", IdeasyCommandlet.ARTIFACT,
      "gcviewer", new MvnArtifact("com.github.chewiebug", "gcviewer", "*")
  );

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public MavenRepository(IdeContext context) {

    super(context);
    this.localMavenRepository = IdeVariables.M2_REPO.get(this.context);
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      this.documentBuilder = factory.newDocumentBuilder();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create XML document builder", e);
    }
  }

  @Override
  public String getId() {

    return ID;
  }

  private MvnArtifact resolveArtifact(String tool, String edition, ToolCommandlet toolCommandlet) {
    MvnArtifact artifact;
    if (toolCommandlet instanceof MvnBasedLocalToolCommandlet mvnBasedTool) {
      artifact = mvnBasedTool.getArtifact(edition);
    } else {
      String key = tool;
      if (!tool.equals(edition)) {
        key = tool + ":" + edition;
      }
      artifact = TOOL_MAP.get(key);
      if (artifact == null) {
        throw new UnsupportedOperationException("Tool '" + key + "' is not supported by Maven repository.");
      }
    }
    return artifact;
  }

  /**
   * @param artifact the {@link MvnArtifact} to resolve.
   * @param tool the {@link MavenArtifactMetadata#getTool() tool name}.
   * @param edition the {@link MavenArtifactMetadata#getEdition() tool edition}.
   * @param version the {@link MavenArtifactMetadata#getVersion() tool version}.
   * @return the resolved {@link MavenArtifactMetadata}.
   */
  public MavenArtifactMetadata resolveArtifact(MvnArtifact artifact, String tool, String edition, VersionIdentifier version) {
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
    UrlChecksums chekcsums = null;
    if (version != null) {
      artifact = artifact.withVersion(version.toString());
      chekcsums = new UrlLazyChecksums(artifact);
    }
    return new MavenArtifactMetadata(artifact, tool, edition, chekcsums, os, arch);
  }

  private UrlGenericChecksum getChecksum(MvnArtifact artifact, String hashAlgorithm) {

    MvnArtifact checksumArtifact = artifact.withType(artifact.getType() + "." + hashAlgorithm.toLowerCase(Locale.ROOT));
    Path checksumFile = getDownloadedArtifact(checksumArtifact, null);
    String checksum = this.context.getFileAccess().readFileContent(checksumFile).trim();
    return new UrlGenericChecksumType(checksum, hashAlgorithm, checksumFile);
  }

  private Path getDownloadedArtifact(MvnArtifact artifact, UrlChecksums checksums) {

    Path file = this.localMavenRepository.resolve(artifact.getPath());
    if (isNotUpToDateInLocalRepo(file)) {
      this.context.getFileAccess().mkdirs(file.getParent());
      download(artifact.getDownloadUrl(), file, artifact.getVersion(), checksums);
    }
    return file;
  }

  private boolean isNotUpToDateInLocalRepo(Path file) {
    if (!Files.exists(file)) {
      return true;
    }
    if (file.getFileName().toString().equals(MvnArtifact.MAVEN_METADATA_XML)) {
      Duration cacheDuration = METADATA_CACHE_DURATION_RELEASE;
      if (file.getParent().getFileName().toString().endsWith("-SNAPSHOT")) {
        cacheDuration = METADATA_CACHE_DURATION_SNAPSHOT;
      }
      return !this.context.getFileAccess().isFileAgeRecent(file, cacheDuration);
    }
    return false;
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    MvnArtifact artifact = resolveArtifact(tool, edition, toolCommandlet);
    return resolveArtifact(artifact, tool, edition, version);
  }


  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet) {

    MvnArtifact artifact = resolveArtifact(tool, edition, toolCommandlet);
    return resolveVersion(artifact, version);
  }

  /**
   * @param artifact the {@link MvnArtifact} to resolve. {@link MvnArtifact#getVersion() Version} should be undefined ("*").
   * @param version the {@link GenericVersionRange} to resolve.
   * @return the resolved {@link VersionIdentifier}.
   */
  public VersionIdentifier resolveVersion(MvnArtifact artifact, GenericVersionRange version) {

    artifact = artifact.withMavenMetadata();
    String versionString = version.toString();
    if (versionString.startsWith("*")) {
      artifact = artifact.withVersion(versionString);
    }
    List<VersionIdentifier> versions = fetchVersions(artifact);
    VersionIdentifier resolvedVersion = VersionIdentifier.resolveVersionPattern(version, versions, this.context);
    versionString = resolvedVersion.toString();
    if (versionString.endsWith("-SNAPSHOT")) {
      artifact = artifact.withVersion(versionString);
      return resolveSnapshotVersion(artifact.getDownloadUrl(), versionString);
    }
    return resolvedVersion;
  }

  private List<VersionIdentifier> fetchVersions(MvnArtifact artifact) {

    String metadataUrl = artifact.withMavenMetadata().getDownloadUrl();
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

  @Override
  public List<String> getSortedEditions(String tool) {

    return List.of(tool);
  }

  @Override
  public List<VersionIdentifier> getSortedVersions(String tool, String edition, ToolCommandlet toolCommandlet) {

    MvnArtifact artifact = resolveArtifact(tool, edition, toolCommandlet);
    return fetchVersions(artifact);
  }

  @Override
  public Collection<ToolDependency> findDependencies(String groupId, String artifactId, VersionIdentifier version) {

    // We could read POM here and find dependencies but we do not want to reimplement maven here.
    // For our use-case we only download bundled packages from maven central so we do KISS for now.
    return Collections.emptyList();
  }

  @Override
  public ToolSecurity findSecurity(String tool, String edition) {
    return ToolSecurity.getEmpty();
  }

  @Override
  public Path download(UrlDownloadFileMetadata metadata) {

    if (metadata instanceof MavenArtifactMetadata mvnMetadata) {
      return download(mvnMetadata);
    }
    return super.download(metadata);
  }

  /**
   * @param metadata the {@link MavenArtifactMetadata}.
   * @return the {@link Path} to the downloaded artifact.
   */
  public Path download(MavenArtifactMetadata metadata) {

    return getDownloadedArtifact(metadata.getMvnArtifact(), metadata.getChecksums());
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

  private class UrlLazyChecksums implements UrlChecksums {

    private final MvnArtifact artifact;

    private volatile List<UrlGenericChecksum> checksums;

    public UrlLazyChecksums(MvnArtifact artifact) {

      super();
      this.artifact = artifact;
    }

    @Override
    public Iterator<UrlGenericChecksum> iterator() {

      if (this.checksums == null) {
        synchronized (this) {
          if (this.checksums == null) {
            UrlGenericChecksum md5 = getChecksum(this.artifact, "MD5");
            UrlGenericChecksum sha1 = getChecksum(this.artifact, "SHA1");
            this.checksums = List.of(md5, sha1);
          }
        }
      }
      return this.checksums.iterator();
    }
  }

}
