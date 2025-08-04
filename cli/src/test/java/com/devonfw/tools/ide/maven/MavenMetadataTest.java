package com.devonfw.tools.ide.maven;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link MavenMetadata}.
 */
class MavenMetadataTest extends Assertions {

  /**
   * Test of constructors, getters, and setters of {@link MavenMetadata}.
   */
  @Test
  void testMavenMetadata() {
    MavenVersioning versioning = new MavenVersioning("1.2.3", "1.2.2", Arrays.asList("1.2.1", "1.2.2", "1.2.3"), "20250729");
    MavenMetadata metadata = new MavenMetadata("com.example", "demo-artifact", versioning);
    assertThat(metadata.getGroupId()).isEqualTo("com.example");
    assertThat(metadata.getArtifactId()).isEqualTo("demo-artifact");
    assertThat(metadata.getVersioning()).isEqualTo(versioning);

    // Test setters
    metadata.setGroupId("org.test");
    metadata.setArtifactId("test-artifact");
    MavenVersioning v2 = new MavenVersioning();
    metadata.setVersioning(v2);
    assertThat(metadata.getGroupId()).isEqualTo("org.test");
    assertThat(metadata.getArtifactId()).isEqualTo("test-artifact");
    assertThat(metadata.getVersioning()).isEqualTo(v2);
  }
}

