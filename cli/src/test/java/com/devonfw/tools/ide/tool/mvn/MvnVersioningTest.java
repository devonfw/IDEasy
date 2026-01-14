package com.devonfw.tools.ide.tool.mvn;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link MvnVersioning}.
 */
class MvnVersioningTest extends Assertions {

  /**
   * Test of constructors, getters, and setters of {@link MvnVersioning}.
   */
  @Test
  void testMavenVersioning() {
    List<String> versions = Arrays.asList("1.0.0", "1.1.0", "2.0.0");
    MvnVersioning versioning = new MvnVersioning("2.0.0", "1.1.0", versions, "20250729");
    assertThat(versioning.getLatest()).isEqualTo("2.0.0");
    assertThat(versioning.getRelease()).isEqualTo("1.1.0");
    assertThat(versioning.getVersions()).isEqualTo(versions);
    assertThat(versioning.getLastUpdated()).isEqualTo("20250729");

    // Test setters
    versioning.setLatest("3.0.0");
    versioning.setRelease("2.0.0");
    versioning.setVersions(Arrays.asList("3.0.0"));
    versioning.setLastUpdated("20250730");
    assertThat(versioning.getLatest()).isEqualTo("3.0.0");
    assertThat(versioning.getRelease()).isEqualTo("2.0.0");
    assertThat(versioning.getVersions()).containsExactly("3.0.0");
    assertThat(versioning.getLastUpdated()).isEqualTo("20250730");
  }
}

