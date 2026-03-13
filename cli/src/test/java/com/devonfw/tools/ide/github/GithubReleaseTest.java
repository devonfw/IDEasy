package com.devonfw.tools.ide.github;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link GithubRelease}.
 */
class GithubReleaseTest extends Assertions {

  /**
   * Test that the ref and version are correctly extracted from a typical release ref string.
   */
  @Test
  void testRefAndVersion() {
    // arrange
    GithubRelease release = new GithubRelease("1.2.3 RC1");

    // assert
    assertThat(release.name()).isEqualTo("1.2.3 RC1");
    assertThat(release.version()).isEqualTo("1.2.3 RC1");
  }

  /**
   * Test that the version is returned as-is when the ref does not have a prefix.
   */
  @Test
  void testVersionWithNoPrefix() {
    // arrange
    GithubRelease release = new GithubRelease("2.0.0");

    // assert
    assertThat(release.version()).isEqualTo("2.0.0");
  }
}
