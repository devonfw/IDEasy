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
public class VersionIdentifierTest extends Assertions {

  /**
   * Test of {@link VersionIdentifier#of(String)}.
   */
  @Test
  public void testOf() {

    // test exotic version number
    // given
    String version = "1.0-release-candidate2_-.HF1";
    // when
    VersionIdentifier vid = VersionIdentifier.of(version);
    // then
    assertThat(vid.isPattern()).isFalse();
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
  public void testValid(String version) {

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
  public void testInvalid(String version) {

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
  public void testCompare() {

    String[] versions = { "0.1", "0.2-SNAPSHOT", "0.2-nb5", "0.2-a", "0.2-alpha1", "0.2-beta", "0.2-b2", "0.2.M1",
        "0.2M9", "0.2M10", "0.2-rc1", "0.2-RC2", "0.2", "0.2-fix9", "0.2-hf1", "0.3", "0.3.1", "1", "1.0", "10-alpha1" };
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
   * Test of {@link VersionIdentifier#matches(VersionIdentifier)} with {@link VersionSegment#PATTERN_MATCH_ANY_STABLE_VERSION}.
   */
  @Test
  public void testMatchStable() {

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
  public void testMatchAny() {

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
  public void testCompareJavaVersions() {

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
  public void testSnapshotStarFindsUnstableVersions() {
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
  public void testSnapshotStarNotMatchingUnstableVersions() {
    VersionIdentifier snapshot_star = VersionIdentifier.of("*-SNAPSHOT");
    assertThat(snapshot_star.isValid()).isFalse();
    assertThat(snapshot_star.isPattern()).isTrue();
    assertThat(snapshot_star.matches(VersionIdentifier.of("2025.03.001-beta-SNAPSHOT"))).isFalse();
    assertThat(snapshot_star.matches(VersionIdentifier.of("2025.03.001-SNAPSHOT"))).isFalse();
  }

}
