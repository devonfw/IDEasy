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
    String version = "2.0.0";
    NpmJsonDist dist = new NpmJsonDist("https://example.com/npm-" + version + ".tgz");

    // act
    NpmJsonVersion npmVersion = new NpmJsonVersion(version, dist);

    // assert
    assertThat(npmVersion.version()).isEqualTo(version);
    assertThat(npmVersion.dist()).isEqualTo(dist);
  }
}

