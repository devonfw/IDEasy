package com.devonfw.tools.ide.github;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link GithubTag}.
 */
class GithubTagTest extends Assertions {

  /**
   * Test that the tag name is returned as version.
   */
  @Test
  void testNameAndVersion() {
    // arrange
    GithubTag tag = new GithubTag("v1.2.3");

    // assert
    assertThat(tag.name()).isEqualTo("v1.2.3");
    assertThat(tag.version()).isEqualTo("v1.2.3");
  }
}
