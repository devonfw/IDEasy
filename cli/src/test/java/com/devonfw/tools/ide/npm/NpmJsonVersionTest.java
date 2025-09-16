package com.devonfw.tools.ide.npm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link NpmJsonVersion}.
 */
class NpmJsonVersionTest extends Assertions {

  /**
   * Test of {@link NpmJsonVersion} constructor and its properties.
   */
  @Test
  void testNpmJsonVersion() {

    // arrange
    String url = "https://registry.npmjs.org/npm/-/npm-2.0.0.tgz";
    String sha1 = "f783874393588901af1a4824a145fa009f174d9d";
    String version = "2.0.0";
    NpmJsonDist dist = new NpmJsonDist(url, sha1);

    // act
    NpmJsonVersion npmVersion = new NpmJsonVersion(version, dist);

    // assert
    assertThat(npmVersion.version()).isEqualTo(version);
    assertThat(npmVersion.dist()).isEqualTo(dist);
  }
}

