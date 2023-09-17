package com.devonfw.tools.ide.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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
  @Test
  public void testValid() {

    String[] validVersions = { "1.0", "0.1", "2023.08.001", "2023-06-M1", "11.0.4_11.4", "5.2.23.RELEASE" };
    for (String version : validVersions) {
      VersionIdentifier vid = VersionIdentifier.of(version);
      assertThat(vid.isValid()).as(version).isTrue();
      assertThat(vid.isPattern()).isFalse();
      assertThat(vid).hasToString(version);
    }
  }

  /**
   * Test of in{@link VersionIdentifier#isValid() valid} versions.
   */
  @Test
  public void testInvalid() {

    String[] invalidVersions = { "0", "0.0", "1.0.pineapple-pen", "1.0-rc", ".1.0", "1.-0", "RC1", "Beta1", "donut" };
    for (String version : invalidVersions) {
      VersionIdentifier vid = VersionIdentifier.of(version);
      assertThat(vid.isValid()).as(version).isFalse();
      assertThat(vid.isPattern()).isFalse();
      assertThat(vid).hasToString(version);
    }
  }

  /**
   * Test of illegal versions.
   */
  @Test
  public void testIllegal() {

    String[] illegalVersions = { "0*.0", "*0", "*.", "17.*alpha", "17*.1" };
    for (String version : illegalVersions) {
      try {
        VersionIdentifier.of(version);
        fail("Illegal verion '" + version + "' did not cause an exception!");
      } catch (Exception e) {
        assertThat(e).isInstanceOf(IllegalArgumentException.class);
        assertThat(e).hasMessageContaining(version);
      }
    }
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
   * Test of {@link VersionIdentifier#matches(VersionIdentifier)} with
   * {@link VersionSegment#PATTERN_MATCH_ANY_STABLE_VERSION}.
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

}
