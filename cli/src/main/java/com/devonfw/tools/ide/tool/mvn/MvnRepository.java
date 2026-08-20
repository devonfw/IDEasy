package com.devonfw.tools.ide.tool.mvn;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.devonfw.tools.ide.cache.CachedValue;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.repository.AbstractToolRepository;
import com.devonfw.tools.ide.tool.repository.ArtifactToolRepository;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.UrlGenericChecksum;
import com.devonfw.tools.ide.url.model.file.UrlGenericChecksumType;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link AbstractToolRepository} for {@link com.devonfw.tools.ide.tool.java.Java java}-based artifacts. Actually
 * {@link com.devonfw.tools.ide.tool.mvn.Mvn maven} was the first famous build management tool with a central repository. Meanwhile, there are others like
 * {@link com.devonfw.tools.ide.tool.gradle.Gradle}. However, it is still called maven-repository and not java-repository.
 */
public class MvnRepository extends ArtifactToolRepository<MvnArtifact, MvnArtifactMetadata> {

  private static final Logger LOG = LoggerFactory.getLogger(MvnRepository.class);

  /** Base URL for Maven Central repository */
  public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  /** Base URL for Maven Snapshots repository */
  public static final String MAVEN_SNAPSHOTS = "https://central.sonatype.com/repository/maven-snapshots";

  /** The {@link #getId() repository ID}. */
  public static final String ID = "maven";

  private static final Duration METADATA_CACHE_DURATION_RELEASE = Duration.ofHours(1);

  private static final Duration METADATA_CACHE_DURATION_SNAPSHOT = Duration.ofMinutes(5);

  /**
   * Short-lived in-memory cache used to avoid duplicate XML metadata fetches within one CLI run.
   */
  private static final Duration XML_METADATA_CACHE_DURATION = Duration.ofMinutes(1);

  private static final String CLASSIFIER_WINDOWS_ARM64 = "windows-arm64";

  private static final String CLASSIFIER_WINDOWS_X64 = "windows-x64";

  private final Path localMavenRepository;

  private final Map<String, CachedValue<Document>> xmlMetadataCache;

  private static final Map<String, MvnArtifact> TOOL_MAP = Map.of(
      "ideasy", IdeasyCommandlet.ARTIFACT,
      "gcviewer", new MvnArtifact("com.github.chewiebug", "gcviewer", "*")
  );

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public MvnRepository(IdeContext context) {

    super(context);
    this.localMavenRepository = IdeVariables.M2_REPO.get(this.context);
    this.xmlMetadataCache = new ConcurrentHashMap<>();
  }

  @Override
  public String getId() {

    return ID;
  }

  @Override
  protected MvnArtifact resolveArtifact(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {
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
    if (version != null) {
      artifact = artifact.withVersion(version.toString());
    }
    return artifact;
  }

  @Override
  public MvnArtifactMetadata getMetadata(MvnArtifact artifact, String tool, String edition) {
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
      } else {
        classifier = resolvedClassifier;
      }

      if (CLASSIFIER_WINDOWS_ARM64.equals(classifier)) {
        classifier = resolveWindowsArm64Classifier(
            artifact,
            classifier,
            artifact.getType());

        if (CLASSIFIER_WINDOWS_X64.equals(classifier)) {
          arch = SystemArchitecture.X64;
        }
      }

      artifact = artifact.withClassifier(classifier);
    }

    UrlChecksums checksums = getChecksums(artifact);
    return new MvnArtifactMetadata(artifact, tool, edition, checksums, os, arch);
  }

  /**
   * Resolves the classifier for a timestamped Maven snapshot.
   * <p>
   * IDEasy currently does not publish a Windows ARM64 artifact. Windows on ARM
   * can execute Windows x64 applications, so the x64 artifact is used as a
   * fallback when the ARM64 artifact is unavailable.
   *
   * @param artifact the artifact being resolved.
   * @param classifier the originally resolved classifier.
   * @param extension the artifact extension, such as {@code tar.gz}.
   * @return the original classifier if it exists or no suitable fallback is available; otherwise {@code windows-x64}.
   */
  private String resolveWindowsArm64Classifier(MvnArtifact artifact, String classifier, String extension) {

    if (!artifact.isSnapshot()) {
      logWindowsArm64Fallback(artifact.getVersion());
      return CLASSIFIER_WINDOWS_X64;
    }

    String metadataUrl = getMavenUrl(artifact.withMavenMetadata());

    try {
      Document metadata = fetchXmlMetadata(metadataUrl);
      return resolveSnapshotClassifier(metadata, classifier, extension, artifact.getVersion());
    } catch (RuntimeException e) {
      LOG.debug("Failed to inspect snapshot metadata from {} - falling back to {}.",
          metadataUrl, CLASSIFIER_WINDOWS_X64, e);
      return CLASSIFIER_WINDOWS_X64;
    }
  }

  /**
   * Resolves a snapshot classifier from Maven snapshot metadata.
   *
   * @param metadata the version-specific Maven snapshot metadata.
   * @param classifier the requested artifact classifier.
   * @param extension the requested artifact extension.
   * @param version the artifact version.
   * @return the resolved classifier.
   */
  String resolveSnapshotClassifier(Document metadata, String classifier, String extension, String version) {

    if (hasSnapshotArtifact(metadata, classifier, extension)) {
      return classifier;
    }

    if (CLASSIFIER_WINDOWS_ARM64.equals(classifier)
        && hasSnapshotArtifact(metadata, CLASSIFIER_WINDOWS_X64, extension)) {

      logWindowsArm64Fallback(version);
      return CLASSIFIER_WINDOWS_X64;
    }

    // Preserve the original classifier so the existing download error is
    // reported when neither the requested artifact nor a fallback exists.
    return classifier;
  }

  /**
   * Checks whether the Maven snapshot metadata contains the requested artifact.
   *
   * @param metadata the Maven snapshot metadata.
   * @param classifier the artifact classifier.
   * @param extension the artifact extension.
   * @return {@code true} if the artifact is present.
   */
  private boolean hasSnapshotArtifact(Document metadata, String classifier, String extension) {

    Element root = metadata.getDocumentElement();

    Element versioning =
        findFirstChildElement(root, MvnArtifact.XML_TAG_VERSIONING);

    if (versioning == null) {
      return false;
    }

    Element snapshotVersions =
        findFirstChildElement(versioning, MvnArtifact.XML_TAG_SNAPSHOT_VERSIONS);

    if (snapshotVersions == null) {
      return false;
    }

    NodeList children = snapshotVersions.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);

      if (node instanceof Element snapshotVersion
          && MvnArtifact.XML_TAG_SNAPSHOT_VERSION.equals(snapshotVersion.getTagName())) {

        String currentClassifier =
            getChildText(snapshotVersion, MvnArtifact.XML_TAG_CLASSIFIER);

        String currentExtension =
            getChildText(snapshotVersion, MvnArtifact.XML_TAG_EXTENSION);

        if (classifier.equals(currentClassifier)
            && extension.equals(currentExtension)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Gets the text content of a direct child node.
   *
   * @param element the parent node.
   * @param tag the child-node name.
   * @return the child text, or {@code null} if the child does not exist.
   */
  private String getChildText(Element element, String tag) {

    Element child = findFirstChildElement(element, tag);
    if (child == null) {
      return null;
    }
    return child.getTextContent().trim();
  }

  /**
   * Method is required to disable checksum checks in tests.
   *
   * @param artifact the {@link MvnArtifact} to use.
   * @return the {@link UrlChecksums}.
   */
  protected UrlChecksums getChecksums(MvnArtifact artifact) {

    UrlChecksums checksums = null;
    if (!artifact.isMavenMetadata()) {
      checksums = new UrlLazyChecksums(artifact);
    }
    return checksums;
  }

  protected UrlGenericChecksum getChecksum(MvnArtifact artifact, String hashAlgorithm) {

    MvnArtifact checksumArtifact = artifact.withType(artifact.getType() + "." + hashAlgorithm.toLowerCase(Locale.ROOT));
    Path checksumFile = getDownloadedArtifact(checksumArtifact, null);
    String checksum = this.context.getFileAccess().readFileContent(checksumFile).trim();
    return new UrlGenericChecksumType(checksum, hashAlgorithm, checksumFile);
  }

  private Path getDownloadedArtifact(MvnArtifact artifact, UrlChecksums checksums) {

    Path file = this.localMavenRepository.resolve(artifact.getPath());
    if (isNotUpToDateInLocalRepo(file)) {
      this.context.getFileAccess().mkdirs(file.getParent());
      download(getMavenUrl(artifact), file, artifact.getVersion(), checksums);
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
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet) {

    MvnArtifact artifact = resolveArtifact(tool, edition, null, toolCommandlet);
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
    VersionIdentifier resolvedVersion = VersionIdentifier.resolveVersionPattern(version, versions);
    versionString = resolvedVersion.toString();
    if (versionString.endsWith("-SNAPSHOT")) {
      artifact = artifact.withVersion(versionString);
      return resolveSnapshotVersion(getMavenUrl(artifact), versionString);
    }
    return resolvedVersion;
  }

  @Override
  protected List<VersionIdentifier> fetchVersions(MvnArtifact artifact) {

    String metadataUrl = getMavenUrl(artifact.withMavenMetadata());

    Document metadata = fetchXmlMetadata(metadataUrl);
    return fetchVersions(metadata, metadataUrl);
  }

  List<VersionIdentifier> fetchVersions(Document metadata, String source) {
    Element versioning = getFirstChildElement(metadata.getDocumentElement(), "versioning", source);
    Element versions = getFirstChildElement(versioning, "versions", source);
    NodeList versionsChildren = versions.getElementsByTagName("version");
    int length = versionsChildren.getLength();
    List<VersionIdentifier> versionList = new ArrayList<>(length);
    for (int i = length - 1; i >= 0; i--) {
      versionList.add(VersionIdentifier.of(versionsChildren.item(i).getTextContent()));
    }
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
  public Path download(UrlDownloadFileMetadata metadata) {

    if (metadata instanceof MvnArtifactMetadata mvnMetadata) {
      return download(mvnMetadata);
    }
    return super.download(metadata);
  }

  /**
   * @param metadata the {@link MvnArtifactMetadata}.
   * @return the {@link Path} to the downloaded artifact.
   */
  public Path download(MvnArtifactMetadata metadata) {

    return getDownloadedArtifact(metadata.getMvnArtifact(), metadata.getChecksums());
  }

  private Element findFirstChildElement(Element element, String tag) {

    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node instanceof Element child && child.getTagName().equals(tag)) {
        return child;
      }
    }
    return null;
  }

  private Element getFirstChildElement(Element element, String tag, Object source) {

    Element child = findFirstChildElement(element, tag);
    if (child != null) {
      return child;
    }
    throw new IllegalStateException(
        "Failed to resolve snapshot version - element " + tag + " not found in " + source);
  }

  private Document fetchXmlMetadata(String url) {

    CachedValue<Document> cachedMetadata =
        this.xmlMetadataCache.computeIfAbsent(
            url,
            key -> new CachedValue<>(
                () -> downloadXmlMetadata(key),
                XML_METADATA_CACHE_DURATION.toMillis()));

    return cachedMetadata.get();
  }

  private Document downloadXmlMetadata(String url) {

    try {
      return this.context.getNetworkStatus().invokeNetworkTask(() -> {
        URL xmlUrl = URI.create(url).toURL();

        try (InputStream is = xmlUrl.openStream()) {
          DocumentBuilder documentBuilder =
              DocumentBuilderFactory.newInstance().newDocumentBuilder();
          return documentBuilder.parse(is);
        }
      }, url);

    } catch (Exception e) {
      throw new CliException(
          "Failed to fetch XML metadata from " + url, e);
    }
  }

  /**
   * Used for tests to overwrite Maven base url.
   *
   * @param artifact the {@link MvnArtifact} to use
   * @return the Maven url
   */
  protected String getMavenUrl(MvnArtifact artifact) {
    return artifact.getDownloadUrl();
  }

  @Override
  protected MvnArtifactMetadata getMetadata(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    return super.getMetadata(tool, edition, version, toolCommandlet);
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

  private void logWindowsArm64Fallback(String version) {
    LOG.warn("No {} artifact is published for version {} - falling back to {} "
            + "(see https://github.com/devonfw/IDEasy/issues/599).",
        CLASSIFIER_WINDOWS_ARM64, version, CLASSIFIER_WINDOWS_X64);
  }
}
