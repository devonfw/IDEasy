package com.devonfw.tools.ide.tool.mvn;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link MvnArtifact}.
 */
class MvnArtifactTest extends Assertions {

  /**
   * Test of {@link MvnArtifact#ofIdeasyCli(String, String, String)}.
   */
  @Test
  void testIdeasyCli() {

    // arrange
    String version = "2025.12.001";
    String type = "tar.gz";
    String classifier = "windows-arm64";
    // act
    MvnArtifact artifact = MvnArtifact.ofIdeasyCli(version, type, classifier);
    MvnArtifact equal = new MvnArtifact(MvnArtifact.GROUP_ID_IDEASY, MvnArtifact.ARTIFACT_ID_IDEASY_CLI, version, type, classifier);
    // assert
    assertThat(artifact.getGroupId()).isEqualTo("com.devonfw.tools.IDEasy");
    assertThat(artifact.getArtifactId()).isEqualTo("ide-cli");
    assertThat(artifact.getVersion()).isEqualTo(version);
    assertThat(artifact.getType()).isEqualTo(type);
    assertThat(artifact.getClassifier()).isEqualTo(classifier);
    assertThat(artifact.getPath()).isEqualTo("com/devonfw/tools/IDEasy/ide-cli/2025.12.001/ide-cli-2025.12.001-windows-arm64.tar.gz");
    assertThat(artifact).hasToString("com.devonfw.tools.IDEasy:ide-cli:2025.12.001:tar.gz:windows-arm64");
    assertThat(artifact.getKey()).isEqualTo(artifact.toString());
    assertThat(artifact.getDownloadUrl()).isEqualTo(
        "https://repo1.maven.org/maven2/com/devonfw/tools/IDEasy/ide-cli/2025.12.001/ide-cli-2025.12.001-windows-arm64.tar.gz");
    assertThat(artifact).isEqualTo(equal);
    assertThat(artifact.hashCode()).isEqualTo(equal.hashCode());
  }

  /**
   * Test of {@link MvnArtifact#ofIdeasyCli(String, String, String)} with SNAPSHOT version.
   */
  @Test
  void testIdeasyCliSnapshot() {

    // arrange
    String version = "2025.01.003-beta-20250130.023001-3";
    String type = "tar.gz";
    String classifier = "windows-x64";
    // act
    MvnArtifact artifact = MvnArtifact.ofIdeasyCli(version, type, classifier);
    MvnArtifact equal = new MvnArtifact(MvnArtifact.GROUP_ID_IDEASY, MvnArtifact.ARTIFACT_ID_IDEASY_CLI, version, type, classifier);
    // assert
    assertThat(artifact.getGroupId()).isEqualTo("com.devonfw.tools.IDEasy");
    assertThat(artifact.getArtifactId()).isEqualTo("ide-cli");
    assertThat(artifact.getVersion()).isEqualTo(version);
    assertThat(artifact.getType()).isEqualTo(type);
    assertThat(artifact.getClassifier()).isEqualTo(classifier);
    assertThat(artifact.getPath()).isEqualTo(
        "com/devonfw/tools/IDEasy/ide-cli/2025.01.003-beta-SNAPSHOT/ide-cli-2025.01.003-beta-20250130.023001-3-windows-x64.tar.gz");
    assertThat(artifact).hasToString("com.devonfw.tools.IDEasy:ide-cli:2025.01.003-beta-20250130.023001-3:tar.gz:windows-x64");
    assertThat(artifact.getDownloadUrl()).isEqualTo(
        "https://central.sonatype.com/repository/maven-snapshots/com/devonfw/tools/IDEasy/ide-cli/2025.01.003-beta-SNAPSHOT/ide-cli-2025.01.003-beta-20250130.023001-3-windows-x64.tar.gz");
    assertThat(artifact.getKey()).isEqualTo(artifact.toString());
    assertThat(artifact).isEqualTo(equal);
    assertThat(artifact.hashCode()).isEqualTo(equal.hashCode());
  }

  /**
   * Test of {@link MvnArtifact#withMavenMetadata()}.
   */
  @Test
  void testMetadata() {

    // arrange
    String groupId = "org.apache.maven.plugins";
    String artifactId = "maven-clean-plugin";
    String version = "*";
    String type = "xml";
    // act
    MvnArtifact artifact = new MvnArtifact(groupId, artifactId, version).withMavenMetadata();
    MvnArtifact equal = new MvnArtifact(groupId, artifactId, version, type, "", MvnArtifact.MAVEN_METADATA_XML);
    // assert
    assertThat(artifact.getGroupId()).isEqualTo(groupId);
    assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
    assertThat(artifact.getVersion()).isEqualTo(version);
    assertThat(artifact.getType()).isEqualTo(type);
    assertThat(artifact.getClassifier()).isEmpty();
    assertThat(artifact.getPath()).isEqualTo("org/apache/maven/plugins/maven-clean-plugin/maven-metadata.xml");
    assertThat(artifact).hasToString("org.apache.maven.plugins:maven-clean-plugin:*:xml");
    assertThat(artifact.getKey()).isEqualTo(artifact.toString());
    assertThat(artifact.getDownloadUrl()).isEqualTo(
        "https://repo1.maven.org/maven2/org/apache/maven/plugins/maven-clean-plugin/maven-metadata.xml");
    assertThat(artifact).isEqualTo(equal);
    assertThat(artifact.hashCode()).isEqualTo(equal.hashCode());
  }

  /**
   * Test of {@link MvnArtifact#withMavenMetadata()}.
   */
  @Test
  void testMetadataWithSnapshot() {

    // arrange
    String groupId = "org.apache.maven.plugins";
    String artifactId = "maven-clean-plugin";
    String version = "*-SNAPSHOT";
    String type = "xml";
    // act
    MvnArtifact artifact = new MvnArtifact(groupId, artifactId, version).withMavenMetadata();
    MvnArtifact equal = new MvnArtifact(groupId, artifactId, version, type, "", MvnArtifact.MAVEN_METADATA_XML);
    // assert
    assertThat(artifact.getGroupId()).isEqualTo(groupId);
    assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
    assertThat(artifact.getVersion()).isEqualTo(version);
    assertThat(artifact.getType()).isEqualTo(type);
    assertThat(artifact.getClassifier()).isEmpty();
    assertThat(artifact.getPath()).isEqualTo("org/apache/maven/plugins/maven-clean-plugin/maven-metadata.xml");
    assertThat(artifact).hasToString("org.apache.maven.plugins:maven-clean-plugin:*-SNAPSHOT:xml");
    assertThat(artifact.getKey()).isEqualTo(artifact.toString());
    assertThat(artifact.getDownloadUrl()).isEqualTo(
        "https://central.sonatype.com/repository/maven-snapshots/org/apache/maven/plugins/maven-clean-plugin/maven-metadata.xml");
    assertThat(artifact).isEqualTo(equal);
    assertThat(artifact.hashCode()).isEqualTo(equal.hashCode());
  }

}
