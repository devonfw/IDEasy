package com.devonfw.tools.ide.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test of {@link VersionIdentifier}.
 */
class VersionIdentifierTest extends Assertions {

  /**
   * Test of {@link VersionIdentifier#of(String)}.
   */
  @Test
  void testOf() {

    // test exotic version number
    // given
    String version = "1.0-release-candidate2_-.HF1";
    // when
    VersionIdentifier vid = VersionIdentifier.of(version);
    // then
    assertThat(vid.isPattern()).isFalse();
    assertThat(vid.getBoundaryType()).isSameAs(BoundaryType.CLOSED);
    VersionSegment segment1 = vid.getStart();
    assertThat(segment1.getSeparator()).isEmpty();
    assertThat(segment1.getLettersString()).isEmpty();
    assertThat(segment1.getPhase()).isSameAs(VersionPhase.NONE);
    assertThat(segment1.getNumber()).isEqualTo(1);
    assertThat(segment1).hasToString("1");
    VersionSegment segment2 = segment1.getNextOrNull();
    assertThat(segment2.getSeparator()).isEqualTo(".");
    assertThat(segment2.getLettersString()).isEmpty();
    assertThat(segment2.getPhase()).isSameAs(VersionPhase.NONE);
    assertThat(segment2.getNumber()).isEqualTo(0);
    assertThat(segment2).hasToString(".0");
    VersionSegment segment3 = segment2.getNextOrNull();
    assertThat(segment3.getSeparator()).isEqualTo("-");
    assertThat(segment3.getLettersString()).isEqualTo("release-candidate");
    assertThat(segment3.getPhase()).isSameAs(VersionPhase.RELEASE_CANDIDATE);
    assertThat(segment3.getNumber()).isEqualTo(2);
    assertThat(segment3).hasToString("-release-candidate2");
    VersionSegment segment4 = segment3.getNextOrNull();
    assertThat(segment4.getSeparator()).isEqualTo("_-.");
    assertThat(segment4.getLettersString()).isEqualTo("HF");
    assertThat(segment4.getPhase()).isSameAs(VersionPhase.HOT_FIX);
    assertThat(segment4.getNumber()).isEqualTo(1);
    assertThat(segment4).hasToString("_-.HF1");

    assertThat(vid.getDevelopmentPhase()).isSameAs(VersionLetters.UNDEFINED);
  }

  /**
   * Test of {@link VersionIdentifier#isValid() valid} versions.
   */
  @ParameterizedTest
  // arrange
  @ValueSource(strings = { "1.0", "0.1", "2023.08.001", "2023-06-M1", "11.0.4_11.4", "5.2.23.RELEASE" })
  void testValid(String version) {

    // act
    VersionIdentifier vid = VersionIdentifier.of(version);

    // assert
    assertThat(vid.isValid()).as(version).isTrue();
    assertThat(vid.isPattern()).isFalse();
    assertThat(vid).hasToString(version);
  }

  /**
   * Test of in{@link VersionIdentifier#isValid() valid} versions.
   */
  @ParameterizedTest
  // arrange
  @ValueSource(strings = { "0", "0.0", "1.0.pineapple-pen", "1.0-rc", ".1.0", "1.-0", "RC1", "Beta1", "donut", "8u412b08", "0*.0", "*0", "*.", "17.*alpha",
      "17*.1" })
  void testInvalid(String version) {

    // act
    VersionIdentifier vid = VersionIdentifier.of(version);

    // assert
    assertThat(vid.isValid()).as(version).isFalse();
    assertThat(vid).hasToString(version);
  }

  /**
   * Test of {@link VersionIdentifier} with canonical version numbers and safe order.
   */
  @Test
  void testCompare() {

    String[] versions = { "0.1", "0.2-SNAPSHOT", "0.2-nb5", "0.2-a", "0.2-alpha1", "0.2-beta", "0.2-b2", "0.2.M1", "0.2M9", "0.2M10", "0.2-rc1", "0.2-RC2",
        "0.2", "0.2-fix9", "0.2-hf1", "0.3", "0.3.1", "1", "1.0", "10-alpha1" };
    List<VersionIdentifier> vids = new ArrayList<>(versions.length);
    for (String version : versions) {
      VersionIdentifier vid = VersionIdentifier.of(version);
      vids.add(vid);
      assertThat(vid).hasToString(version);
    }
    Collections.shuffle(vids);
    Collections.sort(vids);
    for (int i = 0; i < versions.length; i++) {
      String version = versions[i];
      VersionIdentifier vid = vids.get(i);
      assertThat(vid).hasToString(version);
    }
  }

  /**
   * Test of {@link VersionIdentifier#compareVersion(VersionIdentifier)} with {@link VersionComparisonResult#isUnsafe() unsafe results} and other edge-cases.
   */
  @Test
  void testCompareSpecial() {

    assertThat(VersionIdentifier.LATEST.compareVersion(VersionIdentifier.of("2.0"))).isSameAs(VersionComparisonResult.LESS_UNSAFE);
    assertThat(VersionIdentifier.of("2").compareVersion(VersionIdentifier.of("2.0"))).isSameAs(VersionComparisonResult.LESS);
    assertThat(VersionIdentifier.of("2.0").compareVersion(VersionIdentifier.of("2"))).isSameAs(VersionComparisonResult.GREATER);
  }

  /**
   * Test of {@link VersionIdentifier#matches(VersionIdentifier)} with {@link VersionSegment#PATTERN_MATCH_ANY_STABLE_VERSION}.
   */
  @Test
  void testMatchStable() {

    VersionIdentifier pattern = VersionIdentifier.LATEST;
    assertThat(pattern.isValid()).isFalse();
    assertThat(pattern.isPattern()).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17_0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("170"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("171.1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.rc1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.M1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.pre4"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.alpha7"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.beta2"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17-SNAPSHOT"))).isFalse();

    pattern = VersionIdentifier.of("17*");
    assertThat(pattern.isValid()).isFalse();
    assertThat(pattern.isPattern()).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17_0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("170"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("171.1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.rc1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.M1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.pre4"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.alpha7"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.beta2"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17-SNAPSHOT"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("18.0"))).isFalse();
    pattern = VersionIdentifier.of("17.*");
    assertThat(pattern.isValid()).isFalse();
    assertThat(pattern.isPattern()).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17_0.8_7"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("170"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("170.0"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1-rc1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1.M1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1.pre4"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1.pre-alpha7"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1-beta2"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1-SNAPSHOT"))).isFalse();
    pattern = VersionIdentifier.of("17.0*");
    assertThat(pattern.isValid()).isFalse();
    assertThat(pattern.isPattern()).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17_0.8_7"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("170"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("170.0"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0-rc1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.M1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.pre4"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.alpha7"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0-beta2"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0-SNAPSHOT"))).isFalse();
  }

  /**
   * Test of {@link VersionIdentifier#matches(VersionIdentifier)} with {@link VersionSegment#PATTERN_MATCH_ANY_VERSION}.
   */
  @Test
  void testMatchAny() {

    VersionIdentifier pattern = VersionIdentifier.of("17*!");
    assertThat(pattern.isValid()).isFalse();
    assertThat(pattern.isPattern()).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17_0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("170"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("171.1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.rc1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.M1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.pre4"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.alpa7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.beta2"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17-SNAPSHOT"))).isTrue();
    pattern = VersionIdentifier.of("17.*!");
    assertThat(pattern.isValid()).isFalse();
    assertThat(pattern.isPattern()).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17_0.8_7"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("170"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("170.0"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1-rc1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.1.M1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.1.pre4"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.1.alpa7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.1-beta2"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.1-SNAPSHOT"))).isTrue();
    pattern = VersionIdentifier.of("17.0*!");
    assertThat(pattern.isValid()).isFalse();
    assertThat(pattern.isPattern()).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.8_7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17_0.8_7"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.1"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("170"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("170.0"))).isFalse();
    assertThat(pattern.matches(VersionIdentifier.of("17.0-rc1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.M1"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.pre4"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0.alpa7"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0-beta2"))).isTrue();
    assertThat(pattern.matches(VersionIdentifier.of("17.0-SNAPSHOT"))).isTrue();
  }

  @Test
  void testCompareJavaVersions() {

    VersionIdentifier v21_35 = VersionIdentifier.of("21_35");
    VersionIdentifier v21_0_2_13 = VersionIdentifier.of("21.0.2_13");
    VersionIdentifier v21_0_3_9 = VersionIdentifier.of("21.0.3_9");
    assertThat(v21_35).isLessThan(v21_0_2_13);
    assertThat(v21_0_2_13).isLessThan(v21_0_3_9);
    assertThat(v21_0_3_9).isGreaterThan(v21_35);
  }

  /**
   * Tests if unstable SNAPSHOT versions can be detected properly. See: <a href="https://github.com/devonfw/IDEasy/issues/1159">1159</a>
   */
  @Test
  void testSnapshotStarFindsUnstableVersions() {
    VersionIdentifier snapshot_star = VersionIdentifier.of("*!-SNAPSHOT");
    assertThat(snapshot_star.isValid()).isFalse();
    assertThat(snapshot_star.isPattern()).isTrue();
    assertThat(snapshot_star.matches(VersionIdentifier.of("2025.03.001-SNAPSHOT"))).isTrue();
    assertThat(snapshot_star.matches(VersionIdentifier.of("2025.02.001-beta-SNAPSHOT"))).isTrue();
  }

  /**
   * Tests if unstable versions will not match. See: <a href="https://github.com/devonfw/IDEasy/issues/1159">1159</a>
   */
  @Test
  void testSnapshotStarNotMatchingUnstableVersions() {
    VersionIdentifier snapshot_star = VersionIdentifier.of("*-SNAPSHOT");
    assertThat(snapshot_star.isValid()).isFalse();
    assertThat(snapshot_star.isPattern()).isTrue();
    assertThat(snapshot_star.matches(VersionIdentifier.of("2025.03.001-beta-SNAPSHOT"))).isFalse();
    assertThat(snapshot_star.matches(VersionIdentifier.of("2025.03.001-SNAPSHOT"))).isFalse();
  }

  /** Test of {@link VersionIdentifier#incrementSegment(int, boolean)} and related methods. */
  @Test
  void testIncrement() {

    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 0, false, "2.0.0-0.0");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 0, true, "2.0beta.0-0foo.0bar-SNAPSHOT");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 1, false, "1.3.0-0.0");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 1, true, "1.3beta.0-0foo.0bar-SNAPSHOT");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 2, false, "1.2beta.4-0.0");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 2, true, "1.2beta.4-0foo.0bar-SNAPSHOT");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 3, false, "1.2beta.3-5.0");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 3, true, "1.2beta.3-5foo.0bar-SNAPSHOT");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 4, false, "1.2beta.3-4foo.6");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 4, true, "1.2beta.3-4foo.6bar-SNAPSHOT");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 5, false, "1.2beta.3-4foo.5bar-SNAPSHOT");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 5, true, "1.2beta.3-4foo.5bar-SNAPSHOT");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 6, false, "1.2beta.3-4foo.5bar-SNAPSHOT");
    assertIncrement("1.2beta.3-4foo.5bar-SNAPSHOT", 6, true, "1.2beta.3-4foo.5bar-SNAPSHOT");
    // devonfw/IDEasy (we are not planning to implement semantic detection that in this case we should start new segments with 1 instead of 0)
    VersionIdentifier versionIdentifier = VersionIdentifier.of("2025.01.002");
    assertThat(versionIdentifier.incrementMajor(false)).hasToString("2026.00.000");
    assertThat(versionIdentifier.incrementMinor(false)).hasToString("2025.02.000");
    assertThat(versionIdentifier.incrementPatch(false)).hasToString("2025.01.003");
  }

  private static void assertIncrement(String version, int digit, boolean keepLetters, String expectedVersion) {

    VersionIdentifier identifier = VersionIdentifier.of(version);
    VersionIdentifier incremented =
        switch (digit) {
          case 0 -> identifier.incrementMajor(keepLetters);
          case 1 -> identifier.incrementMinor(keepLetters);
          case 2 -> identifier.incrementPatch(keepLetters);
          default -> identifier.incrementSegment(digit, keepLetters);
        };
    assertThat(incremented).hasToString(expectedVersion);
  }

  /** Test of {@link VersionIdentifier#incrementLastDigit(boolean)}. */
  @Test
  void testIncrementLastDigit() {

    assertIncrementLastDigit("1-beta", false, "2");
    assertIncrementLastDigit("1-beta", true, "2-beta");
    assertIncrementLastDigit("1.0-beta", false, "1.1");
    assertIncrementLastDigit("1.0-alpha", true, "1.1-alpha");
    assertIncrementLastDigit("3.2.1_rc-SNAPSHOT", false, "3.2.2");
    assertIncrementLastDigit("3.2.1_rc-SNAPSHOT", true, "3.2.2_rc-SNAPSHOT");
  }

  private static void assertIncrementLastDigit(String version, boolean keepLetters, String expectedVersion) {

    VersionIdentifier identifier = VersionIdentifier.of(version);
    VersionIdentifier incremented = identifier.incrementLastDigit(keepLetters);
    assertThat(incremented).hasToString(expectedVersion);
  }

  /** Test of {@link VersionIdentifier#isStable()}. */
  @Test
  void testIsStable() {

    assertThat(VersionIdentifier.of("2025.01.002").isStable()).isTrue();
    assertThat(VersionIdentifier.of("1.0-rc1").isStable()).isFalse();
    assertThat(VersionIdentifier.of("1.0-alpha1.rc2").isStable()).isFalse();
    assertThat(VersionIdentifier.LATEST.isStable()).isTrue();
    assertThat(VersionIdentifier.LATEST_UNSTABLE.isStable()).isFalse();
  }

}
