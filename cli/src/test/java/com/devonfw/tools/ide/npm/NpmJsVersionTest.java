package com.devonfw.tools.ide.npm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.tool.npm.NpmJsDist;
import com.devonfw.tools.ide.tool.npm.NpmJsVersion;

/**
 * Test of {@link NpmJsVersion}.
 */
class NpmJsVersionTest extends Assertions {

  /**
   * Test of {@link NpmJsVersion} constructor and its properties.
   */
  @Test
  void testNpmJsVersion() {

    // arrange
    String version = "2.0.0";
    NpmJsDist dist = new NpmJsDist("https://registry.npmjs.org/npm/-/npm-" + version + ".tgz");

    // act
    NpmJsVersion npmVersion = new NpmJsVersion(version, dist);

    // assert
    assertThat(npmVersion.version()).isEqualTo(version);
    assertThat(npmVersion.dist()).isEqualTo(dist);
  }
}

