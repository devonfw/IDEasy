package com.devonfw.tools.ide.tool.mvn;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link MvnArtifact}.
 */
public class MvnArtifactTest extends Assertions {

  /**
   * Test of {@link MvnArtifact#getPath()}.
   */
  @Test
  public void testIdeasyCli() {

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
    assertThat(artifact).isEqualTo(equal);
    assertThat(artifact.hashCode()).isEqualTo(equal.hashCode());
  }

}
