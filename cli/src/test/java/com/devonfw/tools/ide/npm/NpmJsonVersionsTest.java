package com.devonfw.tools.ide.npm;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link NpmJsonVersions}.
 */
class NpmJsonVersionsTest extends Assertions {

  /**
   * Test of adding and retrieving versions in {@link NpmJsonVersions}.
   */
  @Test
  void testNpmJsonVersions() {
    // arrange
    NpmJsonVersions versions = new NpmJsonVersions();
    NpmJsonVersion v1 = new NpmJsonVersion("1.0.0", new NpmJsonDist("url1"));
    NpmJsonVersion v2 = new NpmJsonVersion("2.0.0", new NpmJsonDist("url2"));
    versions.setDetails("1.0.0", v1);
    versions.setDetails("2.0.0", v2);
    // act
    Map<String, NpmJsonVersion> map = versions.getVersionMap();
    // assert
    assertThat(map).hasSize(2);
    assertThat(map.get("1.0.0")).isEqualTo(v1);
    assertThat(map.get("2.0.0")).isEqualTo(v2);
  }
}

