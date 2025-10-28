package com.devonfw.tools.ide.github;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link GithubTag}.
 */
class GithubTagTest extends Assertions {

  /**
   * Test that the ref and version are correctly extracted from a typical tag ref string.
   */
  @Test
  void testRefAndVersion() {
    GithubTag tag = new GithubTag("refs/tags/v1.2.3");
    assertThat(tag.ref()).isEqualTo("refs/tags/v1.2.3");
    assertThat(tag.version()).isEqualTo("v1.2.3");
  }

  /**
   * Test that the version is returned as-is when the ref does not have a prefix.
   */
  @Test
  void testVersionWithNoPrefix() {
    GithubTag tag = new GithubTag("v2.0.0");
    assertThat(tag.version()).isEqualTo("v2.0.0");
  }
}
