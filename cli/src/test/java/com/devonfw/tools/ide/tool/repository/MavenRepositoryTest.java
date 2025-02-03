package com.devonfw.tools.ide.tool.repository;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

class MavenRepositoryTest extends AbstractIdeContextTest {

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

}
