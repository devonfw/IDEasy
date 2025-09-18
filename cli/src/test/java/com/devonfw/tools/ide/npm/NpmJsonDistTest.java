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
    String sha1 = "f783874393588901af1a4824a145fa009f174d9d";
    // act
    NpmJsonDist dist = new NpmJsonDist(url, sha1);
    // assert
    assertThat(dist.tarball()).isEqualTo(url);
    assertThat(dist.sha1()).isEqualTo(sha1);
  }
}

