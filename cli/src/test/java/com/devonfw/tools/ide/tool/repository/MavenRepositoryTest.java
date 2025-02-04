package com.devonfw.tools.ide.tool.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

class MavenRepositoryTest extends AbstractIdeContextTest {

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
    MavenRepository mavenRepo = new MavenRepository(context);
    String tool = "ideasy";
    String edition = tool;
    VersionIdentifier version = VersionIdentifier.of("2024.04.001-beta");
    OperatingSystem os = context.getSystemInfo().getOs();
    SystemArchitecture arch = context.getSystemInfo().getArchitecture();

    // act
    UrlDownloadFileMetadata metadata = mavenRepo.getMetadata(tool, edition, version);

    // assert
    assertThat(metadata.getUrls()).containsExactly(
        "https://repo1.maven.org/maven2/com/devonfw/tools/IDEasy/ide-cli/2024.04.001-beta/ide-cli-2024.04.001-beta-" + os + "-" + arch + ".tar.gz");
  }

  @Test
  void testGetMetadataWithSnapshot() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    MavenRepository mavenRepo = new MavenRepository(context);
    String tool = "ideasy";
    String edition = tool;
    VersionIdentifier version = VersionIdentifier.of("2024.04.001-beta-20240419.123456-1");
    OperatingSystem os = context.getSystemInfo().getOs();
    SystemArchitecture arch = context.getSystemInfo().getArchitecture();

    // act
    UrlDownloadFileMetadata metadata = mavenRepo.getMetadata(tool, edition, version);

    // assert
    assertThat(metadata.getUrls()).containsExactly(
        "https://s01.oss.sonatype.org/content/repositories/snapshots/com/devonfw/tools/IDEasy/ide-cli/2024.04.001-beta-SNAPSHOT/ide-cli-2024.04.001-beta-20240419.123456-1-"
            + os + "-" + arch + ".tar.gz");
  }

  /** Test of {@link MavenRepository#resolveSnapshotVersion(Document, String)}. */
  @Test
  void testResolveSnapshotVersion() {

    // arrange
    IdeTestContextMock context = IdeTestContextMock.get();
    MavenRepository mavenRepository = context.getMavenToolRepository();
    Document metadata = parseXml(XML_SNAPSNOT_METADATA);

    // act
    VersionIdentifier version = mavenRepository.resolveSnapshotVersion(metadata, "2025.02.001-beta-SNAPSHOT");

    // assert
    assertThat(version).hasToString("2025.02.001-beta-20250204.023111-1");
  }

  /** Test of {@link MavenRepository#fetchVersions(Document, String)}. */
  @Test
  void testResolveVersion() {

    // arrange
    IdeTestContextMock context = IdeTestContextMock.get();
    MavenRepository mavenRepository = context.getMavenToolRepository();
    Document metadata = parseXml(XML_RELEASE_METADATA);

    // act
    List<VersionIdentifier> versions = mavenRepository.fetchVersions(metadata, "testdata");

    // assert
    assertThat(versions.stream().map(VersionIdentifier::toString)).containsExactly("2025.01.003-beta", "2025.01.002-beta", "2025.01.001-beta",
        "2024.12.002-beta", "2024.12.001-beta", "2024.11.001-beta", "2024.10.001-beta", "2024.09.002-beta", "2024.09.001-beta", "2024.08.001-beta",
        "2024.07.003-alpha", "2024.07.002-alpha", "2024.06.001-alpha", "2024.05.001-alpha", "2024.04.001-alpha", "2024.03.001-alpha");
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
