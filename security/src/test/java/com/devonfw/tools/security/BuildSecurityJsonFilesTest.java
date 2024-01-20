package com.devonfw.tools.security;

import com.devonfw.tools.ide.version.VersionRange;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.devonfw.tools.security.BuildSecurityJsonFiles.getVersionRangeFromInterval;

/**
 * Test of {@link BuildSecurityJsonFiles}.
 */
public class BuildSecurityJsonFilesTest extends Assertions {

  /**
   * Test of {@link BuildSecurityJsonFiles#getVersionRangeFromInterval(String, String, String, String, String)}.
   */
  @Test
  public void testGetVersionRangeFromInterval() {

    // act & assert
    assertThat(getVersionRangeFromInterval("1", null, null, null, null)).isEqualTo(VersionRange.of("[1,)"));
    assertThat(getVersionRangeFromInterval(null, "1", null, null, null)).isEqualTo(VersionRange.of("(1,)"));
    assertThat(getVersionRangeFromInterval(null, null, "1", null, null)).isEqualTo(VersionRange.of("(,1)"));
    assertThat(getVersionRangeFromInterval(null, null, null, "1", null)).isEqualTo(VersionRange.of("(,1]"));
    assertThat(getVersionRangeFromInterval(null, null, null, null, "1")).isEqualTo(VersionRange.of("[1,1]"));
    assertThat(getVersionRangeFromInterval(null, "1", null, "2", "1")).isEqualTo(VersionRange.of("(1,2]"));
  }
}