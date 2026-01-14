package com.devonfw.tools.ide.npm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.tool.npm.NpmJsDist;

/**
 * Test of {@link NpmJsDist}.
 */
class NpmJsDistTest extends Assertions {

  /**
   * Test of {@link NpmJsDist} constructor and tarball property.
   */
  @Test
  void testNpmJsonDist() {
    // arrange
    String url = "https://registry.npmjs.org/npm/-/npm-2.0.0.tgz";
    // act
    NpmJsDist dist = new NpmJsDist(url);
    // assert
    assertThat(dist.tarball()).isEqualTo(url);
  }
}

