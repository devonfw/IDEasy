package com.devonfw.tools.security;

import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.devonfw.tools.security.BuildSecurityJsonFile.getVersionRangeFromInterval;

public class BuildSecurityJsonFileTest extends Assertions {

  /***
   * Test of {@link BuildSecurityJsonFile#getVersionRangeFromInterval(List, String, String, String, String)} and passing
   * vStartExcluding and null for the other parameters .
   */
  @Test
  public void testGetVersionRangeFromIntervalStartExcluding() {

    // arrange
    List<VersionIdentifier> v = getSortedVersions();

    // act & assert
    assertThat(getVersionRangeFromInterval(v, null, null, null, null)).isEqualTo(VersionRange.of(">"));
    assertThat(getVersionRangeFromInterval(v, "1", null, null, null)).isEqualTo(VersionRange.of("1.2.3>"));
    assertThat(getVersionRangeFromInterval(v, "1.2.3", null, null, null)).isEqualTo(VersionRange.of("1.2.4>"));
    assertThat(getVersionRangeFromInterval(v, "1.4", null, null, null)).isEqualTo(VersionRange.of("2.0>"));
    assertThat(getVersionRangeFromInterval(v, "1.5", null, null, null)).isEqualTo(VersionRange.of("2.0>"));
    assertThat(getVersionRangeFromInterval(v, "2.1", null, null, null)).isNull();
    assertThat(getVersionRangeFromInterval(v, "2.2", null, null, null)).isNull();

  }
  /***
   * Test of {@link BuildSecurityJsonFile#getVersionRangeFromInterval(List, String, String, String, String)} and passing
   * vStartIncluding and null for the other parameters .
   */
  @Test
  public void testGetVersionRangeFromIntervalStartIncluding() {

    // arrange
    List<VersionIdentifier> v = getSortedVersions();

    // act & assert
    assertThat(getVersionRangeFromInterval(v, null, "1", null, null)).isEqualTo(VersionRange.of("1.2.3>"));
    assertThat(getVersionRangeFromInterval(v, null, "1.2.3", null, null)).isEqualTo(VersionRange.of("1.2.3>"));
    assertThat(getVersionRangeFromInterval(v, null, "1.4", null, null)).isEqualTo(VersionRange.of("1.4>"));
    assertThat(getVersionRangeFromInterval(v, null, "1.5", null, null)).isEqualTo(VersionRange.of("2.0>"));
    assertThat(getVersionRangeFromInterval(v, null, "2.1", null, null)).isEqualTo(VersionRange.of("2.1>"));
    assertThat(getVersionRangeFromInterval(v, null, "2.2", null, null)).isNull();

  }

  /***
   * Test of {@link BuildSecurityJsonFile#getVersionRangeFromInterval(List, String, String, String, String)} and passing
   * vEndIncluding and null for the other parameters .
   */
  @Test
  public void testGetVersionRangeFromIntervalEndIncluding() {

    // arrange
    List<VersionIdentifier> v = getSortedVersions();

    // act & assert
    assertThat(getVersionRangeFromInterval(v, null, null, "1", null)).isNull();
    assertThat(getVersionRangeFromInterval(v, null, null, "1.2.3", null)).isEqualTo(VersionRange.of(">1.2.3"));
    assertThat(getVersionRangeFromInterval(v, null, null, "1.4", null)).isEqualTo(VersionRange.of(">1.4"));
    assertThat(getVersionRangeFromInterval(v, null, null, "1.5", null)).isEqualTo(VersionRange.of(">1.4"));
    assertThat(getVersionRangeFromInterval(v, null, null, "2.1", null)).isEqualTo(VersionRange.of(">2.1"));
    assertThat(getVersionRangeFromInterval(v, null, null, "2.2", null)).isEqualTo(VersionRange.of(">2.1"));

  }

  /***
   * Test of {@link BuildSecurityJsonFile#getVersionRangeFromInterval(List, String, String, String, String)} and passing
   * vEndExcluding and null for the other parameters .
   */
  @Test
  public void testGetVersionRangeFromIntervalEndExcluding() {

    // arrange
    List<VersionIdentifier> v = getSortedVersions();

    // act & assert
    assertThat(getVersionRangeFromInterval(v, null, null, null, " 1")).isNull();
    assertThat(getVersionRangeFromInterval(v, null, null, null, "1.2.3")).isNull();
    assertThat(getVersionRangeFromInterval(v, null, null, null, "1.4")).isEqualTo(VersionRange.of(">1.3"));
    assertThat(getVersionRangeFromInterval(v, null, null, null, "1.5")).isEqualTo(VersionRange.of(">1.4"));
    assertThat(getVersionRangeFromInterval(v, null, null, null, "2.1")).isEqualTo(VersionRange.of(">2.0"));
    assertThat(getVersionRangeFromInterval(v, null, null, null, "2.2")).isEqualTo(VersionRange.of(">2.1"));

  }

  private static List<VersionIdentifier> getSortedVersions() {

    List<VersionIdentifier> sortedVersions = new ArrayList<>();
    sortedVersions.add(VersionIdentifier.of("2.1"));
    sortedVersions.add(VersionIdentifier.of("2.0"));
    sortedVersions.add(VersionIdentifier.of("1.4"));
    sortedVersions.add(VersionIdentifier.of("1.3"));
    sortedVersions.add(VersionIdentifier.of("1.2.5"));
    sortedVersions.add(VersionIdentifier.of("1.2.4"));
    sortedVersions.add(VersionIdentifier.of("1.2.3"));
    return sortedVersions;
  }
}
