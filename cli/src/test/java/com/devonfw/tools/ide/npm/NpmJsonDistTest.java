package com.devonfw.tools.ide.npm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link NpmJsonDist}.
 */
class NpmJsonDistTest extends Assertions {

  /**
   * Test of {@link NpmJsonDist} constructor and tarball property.
   */
  @Test
  void testNpmJsonDist() {
    // arrange
    String url = "https://registry.npmjs.org/npm/-/npm-2.0.0.tgz";
    // act
    NpmJsonDist dist = new NpmJsonDist(url);
    // assert
    assertThat(dist.tarball()).isEqualTo(url);
  }
}

