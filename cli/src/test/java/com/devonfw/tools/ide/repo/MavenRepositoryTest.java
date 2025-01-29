package com.devonfw.tools.ide.repo;


import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

import org.junit.jupiter.api.Test;

class MavenRepositoryTest extends AbstractIdeContextTest {


  @Test
  void testGetMetadataWithRelease() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    MavenRepository mavenRepo = new MavenRepository(context);
    String groupId = "ideasy";
    String artifactId = "ide-cli";
    VersionIdentifier version = VersionIdentifier.of("2024.04.001-beta");
    String classifier = "windows-x64";
    String extension = "tar.gz";

    // act
    UrlDownloadFileMetadata metadata = mavenRepo.getMetadata(groupId, artifactId, version, classifier, extension);

    // assert
    assertThat(metadata.getUrls()).containsExactly("https://repo1.maven.org/maven2/com/devonfw/tools/IDEasy/ide-cli/2024.04.001-beta/ide-cli-2024.04.001-beta-windows-x64.tar.gz");
  }

  @Test
  void testGetMetadataWithSnapshot() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    MavenRepository mavenRepo = new MavenRepository(context);
    String groupId = "ideasy";
    String artifactId = "ide-cli";
    VersionIdentifier version = VersionIdentifier.of("2024.04.001-beta-20240419.123456-1");
    String classifier = "windows-x64";
    String extension = "tar.gz";

    // act
    UrlDownloadFileMetadata metadata = mavenRepo.getMetadata(groupId, artifactId, version, classifier, extension);

    // assert
    assertThat(metadata.getUrls()).containsExactly("https://s01.oss.sonatype.org/content/repositories/snapshots/com/devonfw/tools/IDEasy/ide-cli/2024.04.001-beta-SNAPSHOT/ide-cli-2024.04.001-beta-20240419.123456-1-windows-x64.tar.gz");
  }

  @Test
  void testGetMetadataWithDefaultExtension() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    MavenRepository mavenRepo = new MavenRepository(context);
    String groupId = "ideasy";
    String artifactId = "ide-cli";
    VersionIdentifier version = VersionIdentifier.of("2024.04.001-beta");
    String classifier = "windows-x64";
    String extension = null;

    // act
    UrlDownloadFileMetadata metadata = mavenRepo.getMetadata(groupId, artifactId, version, classifier, extension);

    // assert
    assertThat(metadata.getUrls()).containsExactly("https://repo1.maven.org/maven2/com/devonfw/tools/IDEasy/ide-cli/2024.04.001-beta/ide-cli-2024.04.001-beta-windows-x64.jar");
  }



}
