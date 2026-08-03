package com.devonfw.tools.ide.tool.npm;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link NpmJsVersions}.
 */
class NpmJsVersionsTest extends Assertions {

  /**
   * Test of adding and retrieving versions in {@link NpmJsVersions}.
   */
  @Test
  void testNpmJsVersions() {
    // arrange
    NpmJsVersions versions = new NpmJsVersions();
    NpmJsVersion v1 = new NpmJsVersion("1.0.0", new NpmJsDist("url1"));
    NpmJsVersion v2 = new NpmJsVersion("2.0.0", new NpmJsDist("url2"));
    versions.setDetails("1.0.0", v1);
    versions.setDetails("2.0.0", v2);
    // act
    Map<String, NpmJsVersion> map = versions.getVersionMap();
    // assert
    assertThat(map).hasSize(2);
    assertThat(map.get("1.0.0")).isEqualTo(v1);
    assertThat(map.get("2.0.0")).isEqualTo(v2);
  }
}

