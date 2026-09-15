package com.devonfw.tools.ide.tool.mvn;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.UrlGenericChecksum;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link MvnRepository}.
 */
class MvnRepositoryTest extends AbstractIdeContextTest {

  private static final String XML_SNAPSNOT_METADATA = """
      <?xml version="1.0" encoding="UTF-8"?>
      <metadata modelVersion="1.1.0">
        <groupId>com.devonfw.tools.IDEasy</groupId>
        <artifactId>ide-cli</artifactId>
        <versioning>
          <lastUpdated>20250204023111</lastUpdated>
          <snapshot>
            <timestamp>20250204.023111</timestamp>
            <buildNumber>1</buildNumber>
          </snapshot>
          <snapshotVersions>
            <snapshotVersion>
              <extension>jar</extension>
              <value>2025.02.001-beta-20250204.023111-1</value>
              <updated>20250204023111</updated>
            </snapshotVersion>
            <snapshotVersion>
              <extension>pom</extension>
              <value>2025.02.001-beta-20250204.023111-1</value>
              <updated>20250204023111</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>mac-x64</classifier>
              <extension>tar.gz</extension>
              <value>2025.02.001-beta-20250204.023111-1</value>
              <updated>20250204023111</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>sources</classifier>
              <extension>jar</extension>
              <value>2025.02.001-beta-20250204.023111-1</value>
              <updated>20250204023111</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>javadoc</classifier>
              <extension>jar</extension>
              <value>2025.02.001-beta-20250204.023111-1</value>
              <updated>20250204023111</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>windows-x64</classifier>
              <extension>tar.gz</extension>
              <value>2025.02.001-beta-20250204.023111-1</value>
              <updated>20250204023111</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>mac-arm</classifier>
              <extension>tar.gz</extension>
              <value>2025.02.001-beta-20250204.023111-1</value>
              <updated>20250204023111</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>linux-x64</classifier>
              <extension>tar.gz</extension>
              <value>2025.02.001-beta-20250204.023111-1</value>
              <updated>20250204023111</updated>
            </snapshotVersion>
          </snapshotVersions>
        </versioning>
        <version>2025.02.001-beta-SNAPSHOT</version>
      </metadata>
      """;

  private static final String XML_RELEASE_METADATA = """
      <?xml version="1.0" encoding="UTF-8"?>
      <metadata>
        <groupId>com.devonfw.tools.IDEasy</groupId>
        <artifactId>ide-cli</artifactId>
        <versioning>
          <latest>2025.01.003-beta</latest>
          <release>2025.01.003-beta</release>
          <versions>
            <version>2024.03.001-alpha</version>
            <version>2024.04.001-alpha</version>
            <version>2024.05.001-alpha</version>
            <version>2024.06.001-alpha</version>
            <version>2024.07.002-alpha</version>
            <version>2024.07.003-alpha</version>
            <version>2024.08.001-beta</version>
            <version>2024.09.001-beta</version>
            <version>2024.09.002-beta</version>
            <version>2024.10.001-beta</version>
            <version>2024.11.001-beta</version>
            <version>2024.12.001-beta</version>
            <version>2024.12.002-beta</version>
            <version>2025.01.001-beta</version>
            <version>2025.01.002-beta</version>
            <version>2025.01.003-beta</version>
          </versions>
          <lastUpdated>20250131120926</lastUpdated>
        </versioning>
      </metadata>
      """;

  /** Set to {@code true} to include checksum verification. However, this requires online access to maven. */
  private static final boolean CHECK_MAVEN_CHECKSUMS = false;

  private static final DocumentBuilder DOCUMENT_BUILDER;

  static {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DOCUMENT_BUILDER = factory.newDocumentBuilder();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create XML document builder", e);
    }
  }

  @Test
  void testGetMetadataWithRelease() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    MvnRepository mavenRepo = new MvnRepository(context);
    String tool = "ideasy";
    String edition = tool;
    VersionIdentifier version = VersionIdentifier.of("2025.01.001-beta");
    OperatingSystem os = context.getSystemInfo().getOs();
    SystemArchitecture arch = context.getSystemInfo().getArchitecture();
    ToolCommandlet toolCommandlet = null;

    // this triggers maven download of checksums. Should we fake the checksum files into mocked maven repo in arrange phase?
    // act
    UrlDownloadFileMetadata metadata = mavenRepo.getMetadata(tool, edition, version, toolCommandlet);

    // assert
    assertThat(metadata.getUrls()).containsExactly(
        "https://repo1.maven.org/maven2/com/devonfw/tools/IDEasy/ide-cli/2025.01.001-beta/ide-cli-2025.01.001-beta-" + os + "-" + arch + ".tar.gz");
    assertThat(metadata.getTool()).isEqualTo(tool);
    assertThat(metadata.getEdition()).isEqualTo(edition);
    assertThat(metadata.getOs()).isSameAs(os);
    assertThat(metadata.getArch()).isSameAs(arch);
    assertThat(metadata.getVersion()).isEqualTo(version);
    if (CHECK_MAVEN_CHECKSUMS) {
      UrlChecksums checksums = metadata.getChecksums();
      Iterator<UrlGenericChecksum> iterator = checksums.iterator();
      assertThat(iterator.hasNext()).isTrue();
      UrlGenericChecksum md5 = iterator.next();
      assertThat(md5.getHashAlgorithm()).isEqualTo("MD5");
      assertThat(md5.getChecksum()).isEqualTo("d77670c1649b8ce226f9c642f4fb90cc");
      assertThat(iterator.hasNext()).isTrue();
      UrlGenericChecksum sha1 = iterator.next();
      assertThat(sha1.getHashAlgorithm()).isEqualTo("SHA1");
      assertThat(sha1.getChecksum()).isEqualTo("d38472e3281093ff1b54481bde838393f136a39c");
      assertThat(iterator.hasNext()).isFalse();
    }
  }

  @Test
  void testGetMetadataWithSnapshot() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    MvnRepository mavenRepo = new MvnRepository(context);
    String tool = "ideasy";
    String edition = tool;
    VersionIdentifier version = VersionIdentifier.of("2025.01.001-beta-20250121.023134-9");
    OperatingSystem os = context.getSystemInfo().getOs();
    SystemArchitecture arch = context.getSystemInfo().getArchitecture();
    ToolCommandlet toolCommandlet = null;

    // act
    UrlDownloadFileMetadata metadata = mavenRepo.getMetadata(tool, edition, version, toolCommandlet);

    // assert
    assertThat(metadata.getUrls()).containsExactly(
        "https://central.sonatype.com/repository/maven-snapshots/com/devonfw/tools/IDEasy/ide-cli/"
            + "2025.01.001-beta-SNAPSHOT/ide-cli-2025.01.001-beta-20250121.023134-9-"
            + os + "-" + arch + ".tar.gz");
    assertThat(metadata.getTool()).isEqualTo(tool);
    assertThat(metadata.getEdition()).isEqualTo(edition);
    assertThat(metadata.getOs()).isSameAs(os);
    assertThat(metadata.getArch()).isSameAs(arch);
    assertThat(metadata.getVersion()).isEqualTo(version);
    if (CHECK_MAVEN_CHECKSUMS) {
      UrlChecksums checksums = metadata.getChecksums();
      Iterator<UrlGenericChecksum> iterator = checksums.iterator();
      assertThat(iterator.hasNext()).isTrue();
      UrlGenericChecksum md5 = iterator.next();
      assertThat(md5.getHashAlgorithm()).isEqualTo("MD5");
      assertThat(md5.getChecksum()).isEqualTo("f174d6eb28d77621cada8e51e8c0c2e0");
      assertThat(iterator.hasNext()).isTrue();
      UrlGenericChecksum sha1 = iterator.next();
      assertThat(sha1.getHashAlgorithm()).isEqualTo("SHA1");
      assertThat(sha1.getChecksum()).isEqualTo("9e15d3a440b61e79614357837e533d1517639a1d");
      assertThat(iterator.hasNext()).isFalse();
    }
  }

  /** Test of {@link MvnRepository#resolveSnapshotVersion(Document, String, String)}. */
  @Test
  void testResolveSnapshotVersion() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    MvnRepository mvnRepository = context.getMvnRepository();
    Document metadata = parseXml(XML_SNAPSNOT_METADATA);

    // act
    VersionIdentifier version = mvnRepository.resolveSnapshotVersion(metadata, "2025.02.001-beta-SNAPSHOT", "testdata");

    // assert
    assertThat(version).hasToString("2025.02.001-beta-20250204.023111-1");
  }

  /** Test of {@link MvnRepository#resolveSnapshotClassifier(Document, String, String, String)}. */
  @Test
  void testResolveSnapshotClassifierFallsBackFromWindowsArm64ToX64() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    MvnRepository mvnRepository = context.getMvnRepository();
    Document metadata = parseXml(XML_SNAPSNOT_METADATA);

    // act
    String classifier = mvnRepository.resolveSnapshotClassifier(
        metadata,
        "windows-arm64",
        "tar.gz",
        "2025.02.001-beta-20250204.023111-1");

    // assert
    assertThat(classifier).isEqualTo("windows-x64");
  }

  /** Test of {@link MvnRepository#resolveSnapshotClassifier(Document, String, String, String)}. */
  @Test
  void testResolveSnapshotClassifierKeepsWindowsArm64WhenAvailable() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    MvnRepository mvnRepository = context.getMvnRepository();

    String arm64Artifact = """
      <snapshotVersion>
        <classifier>windows-arm64</classifier>
        <extension>tar.gz</extension>
        <value>2025.02.001-beta-20250204.023111-1</value>
        <updated>20250204023111</updated>
      </snapshotVersion>
      """;

    String xml = XML_SNAPSNOT_METADATA.replace(
        "</snapshotVersions>",
        arm64Artifact + "</snapshotVersions>");

    Document metadata = parseXml(xml);

    // act
    String classifier = mvnRepository.resolveSnapshotClassifier(
        metadata,
        "windows-arm64",
        "tar.gz",
        "2025.02.001-beta-20250204.023111-1");

    // assert
    assertThat(classifier).isEqualTo("windows-arm64");
  }

  /** Test of {@link MvnRepository#resolveSnapshotClassifier(Document, String, String, String)}. */
  @Test
  void testResolveSnapshotClassifierFallsBackForLinuxArm64() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    MvnRepository mvnRepository = context.getMvnRepository();
    Document metadata = parseXml(XML_SNAPSNOT_METADATA);

    // act
    String classifier = mvnRepository.resolveSnapshotClassifier(
        metadata,
        "linux-arm64",
        "tar.gz",
        "2025.02.001-beta-20250204.023111-1");

    // assert
    assertThat(classifier).isEqualTo("linux-x64");
  }

  /** Test of {@link MvnRepository#fetchVersions(Document, String)}. */
  @Test
  void testResolveVersion() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    MvnRepository mvnRepository = context.getMvnRepository();
    Document metadata = parseXml(XML_RELEASE_METADATA);

    // act
    List<VersionIdentifier> versions = mvnRepository.fetchVersions(metadata, "testdata");
    versions.sort(Comparator.reverseOrder());

    // assert
    assertThat(versions.stream().map(VersionIdentifier::toString)).containsExactly("2025.01.003-beta", "2025.01.002-beta", "2025.01.001-beta",
        "2024.12.002-beta", "2024.12.001-beta", "2024.11.001-beta", "2024.10.001-beta", "2024.09.002-beta", "2024.09.001-beta", "2024.08.001-beta",
        "2024.07.003-alpha", "2024.07.002-alpha", "2024.06.001-alpha", "2024.05.001-alpha", "2024.04.001-alpha", "2024.03.001-alpha");
  }

  /**
   * Tests that a Windows ARM64 release falls back directly to x64.
   */
  @Test
  void testGetMetadataKeepsWindowsArm64ForReleaseOnWindowsArm64() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_ARM64);
    MvnRepository repository = context.getMvnRepository();
    VersionIdentifier version = VersionIdentifier.of("2025.01.001-beta");

    // act
    UrlDownloadFileMetadata metadata = repository.getMetadata(
        "ideasy",
        "ideasy",
        version,
        null);

    // assert
    assertThat(metadata.getUrls())
        .anyMatch(url -> url.endsWith("-windows-arm64.tar.gz"));
    assertThat(metadata.getArch()).isSameAs(SystemArchitecture.ARM64);
  }

  /**
   * Tests that an unavailable ARM64 release artifact falls back to the corresponding x64 artifact.
   */
  @Test
  void testDownloadFallsBackFromArm64ToX64OnNotFound() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    List<String> requestedUrls = new ArrayList<>();

    MvnRepository repository = new MvnRepository(context) {

      @Override
      protected UrlChecksums getChecksums(MvnArtifact artifact) {
        return null;
      }

      @Override
      protected Path download(String url, Path target, Object resolvedVersion, UrlChecksums expectedChecksums) {

        requestedUrls.add(url);

        if (url.contains("windows-arm64")) {
          throw new IllegalStateException("Download failed with status code 404");
        }

        return target;
      }
    };

    MvnArtifact artifact = new MvnArtifact(
        "com.devonfw.tools.IDEasy",
        "ide-cli",
        "2025.01.001-beta")
        .withType("tar.gz")
        .withClassifier("windows-arm64");

    MvnArtifactMetadata metadata =
        repository.getMetadata(artifact, "ideasy", "ideasy");

    // act
    Path result = repository.download(metadata);

    // assert
    assertThat(requestedUrls).hasSize(2);
    assertThat(requestedUrls.get(0)).endsWith("-windows-arm64.tar.gz");
    assertThat(requestedUrls.get(1)).endsWith("-windows-x64.tar.gz");
    assertThat(result.getFileName().toString()).endsWith("-windows-x64.tar.gz");
  }

  /**
   * Tests that an ARM64 release artifact does not fall back to x64 for a non-404 download error.
   */
  @Test
  void testDownloadDoesNotFallbackFromArm64ToX64OnOtherError() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    List<String> requestedUrls = new ArrayList<>();

    MvnRepository repository = new MvnRepository(context) {

      @Override
      protected UrlChecksums getChecksums(MvnArtifact artifact) {
        return null;
      }

      @Override
      protected Path download(String url, Path target, Object resolvedVersion, UrlChecksums expectedChecksums) {

        requestedUrls.add(url);
        throw new IllegalStateException("Network connection failed");
      }
    };

    MvnArtifact artifact = new MvnArtifact(
        "com.devonfw.tools.IDEasy",
        "ide-cli",
        "2025.01.001-beta")
        .withType("tar.gz")
        .withClassifier("windows-arm64");

    MvnArtifactMetadata metadata =
        repository.getMetadata(artifact, "ideasy", "ideasy");

    // act + assert
    assertThatThrownBy(() -> repository.download(metadata))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Network connection failed");

    assertThat(requestedUrls).hasSize(1);
    assertThat(requestedUrls.get(0)).endsWith("-windows-arm64.tar.gz");
  }

  private static Document parseXml(String xml) {

    InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
    try {
      return DOCUMENT_BUILDER.parse(inputStream);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse XML!", e);
    }
  }
}
