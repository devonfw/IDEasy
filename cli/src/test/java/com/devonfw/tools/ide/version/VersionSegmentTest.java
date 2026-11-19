package com.devonfw.tools.ide.version;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link VersionSegment}.
 */
class VersionSegmentTest extends Assertions {

  /**
   * Test of {@link VersionSegment#ofEmpty()} and {@link VersionSegment#isEmpty()}.
   */
  @Test
  void testEmpty() {

    // given + when
    VersionSegment segment = VersionSegment.ofEmpty();
    // then
    assertThat(segment.isEmpty()).isTrue();
    assertThat(segment.getSeparator()).isEmpty();
    assertThat(segment.getLettersString()).isEmpty();
    assertThat(segment.getPhase()).isSameAs(VersionPhase.NONE);
    assertThat(segment.getDigits()).isEmpty();
    assertThat(segment.getNumber()).isEqualTo(-1);
    assertThat(segment.getPattern()).isEmpty();
    assertThat(segment.isPattern()).isFalse();
    assertThat(VersionSegment.ofEmpty()).isSameAs(segment);
    assertThat(segment.getNextOrNull()).isNull();
    assertThat(segment.getNextOrEmpty()).isSameAs(segment);
  }

  /**
   * Test of constructor and getters with "01".
   */
  @Test
  void testOne() {

    VersionSegment segment = new VersionSegment("", "", "01");
    assertThat(segment.getSeparator()).isEmpty();
    assertThat(segment.getLettersString()).isEmpty();
    assertThat(segment.getPhase()).isSameAs(VersionPhase.NONE);
    assertThat(segment.getDigits()).isEqualTo("01");
    assertThat(segment.getNumber()).isEqualTo(1);
    assertThat(segment.getPattern()).isEmpty();
    assertThat(segment.isPattern()).isFalse();
    assertThat(segment.getNextOrNull()).isNull();
    assertThat(segment.getNextOrEmpty()).isSameAs(VersionSegment.ofEmpty());
  }

  /**
   * Test of constructor and getters with ".rc1".
   */
  @Test
  void testDotRc2() {

    VersionSegment segment = new VersionSegment(".", "rc", "1");
    assertThat(segment.getSeparator()).isEqualTo(".");
    assertThat(segment.getLettersString()).isEqualTo("rc");
    assertThat(segment.getPhase()).isSameAs(VersionPhase.RELEASE_CANDIDATE);
    assertThat(segment.getDigits()).isEqualTo("1");
    assertThat(segment.getNumber()).isEqualTo(1);
    assertThat(segment.getPattern()).isEmpty();
    assertThat(segment.isPattern()).isFalse();
    assertThat(segment.getNextOrNull()).isNull();
    assertThat(segment.getNextOrEmpty()).isSameAs(VersionSegment.ofEmpty());
  }

  /**
   * Test of constructor and getters with ".17*".
   */
  @Test
  void testPattern() {

    VersionSegment segment = new VersionSegment(".", "", "17", "*");
    assertThat(segment.getSeparator()).isEqualTo(".");
    assertThat(segment.getLettersString()).isEqualTo("");
    assertThat(segment.getPhase()).isSameAs(VersionPhase.NONE);
    assertThat(segment.getDigits()).isEqualTo("17");
    assertThat(segment.getNumber()).isEqualTo(17);
    assertThat(segment.getPattern()).isEqualTo("*");
    assertThat(segment.isPattern()).isTrue();
    assertThat(segment.getNextOrNull()).isNull();
    assertThat(segment.getNextOrEmpty()).isSameAs(VersionSegment.ofEmpty());
  }

  /**
   * Test that an int overflow of the {@link VersionSegment#getNumber() number} does not fail parsing but clamps the number to {@link VersionSegment#MAX_NUMBER}
   * while the full {@link VersionSegment#getDigits() digits} are preserved.
   */
  @Test
  void testOverflowNumberClamped() {

    VersionSegment segment = new VersionSegment("", "", "899999999999");

    assertThat(segment.isOverflow()).isTrue();
    assertThat(segment.getNumber()).isEqualTo(VersionSegment.MAX_NUMBER);
    assertThat(segment.getDigits()).isEqualTo("899999999999");
    // the digits are preserved so the version can be rebuilt as String
    assertThat(segment).hasToString("899999999999");
    assertThat(VersionIdentifier.ofRequired("0.0.899999999999").toString()).isEqualTo("0.0.899999999999");
  }

  /**
   * Test that the {@link VersionSegment#isOverflow()} flag is {@code true} exactly when the digits do not fit into an {@code int}.
   */
  @Test
  void testOverflowFlag() {

    assertThat(new VersionSegment("", "", "0").isOverflow()).isFalse();
    assertThat(new VersionSegment("", "", "123456789").isOverflow()).isFalse();
    assertThat(new VersionSegment("", "", String.valueOf(Integer.MAX_VALUE)).isOverflow()).isFalse();
    assertThat(new VersionSegment("", "", String.valueOf(Integer.MAX_VALUE + 1L)).isOverflow()).isTrue();
    assertThat(new VersionSegment("", "", "12345678901234567890").isOverflow()).isTrue();
  }

  /**
   * Test that {@link VersionSegment#of(String)} and {@link VersionIdentifier#ofRequired(String)} handle segments with an int overflow without throwing and keep
   * the version in its string representation.
   */
  @Test
  void testParseOverflowVersion() {

    VersionSegment segment = VersionSegment.of("9876543210");

    assertThat(segment).isNotNull();
    assertThat(segment.isOverflow()).isTrue();
    assertThat(segment.getNumber()).isEqualTo(VersionSegment.MAX_NUMBER);
    assertThat(segment.getDigits()).isEqualTo("9876543210");
    assertThat(segment).hasToString("9876543210");
  }

  /**
   * Test that two different overflowed segments (whose numbers both clamp to {@link VersionSegment#MAX_NUMBER}) are still compared correctly by their digits
   * and do not become equal.
   */
  @Test
  void testCompareOverflowedVersions() {

    VersionSegment v888 = VersionSegment.of("8888888888");
    VersionSegment v999 = VersionSegment.of("9999999999");
    assertThat(v888.getNumber()).isEqualTo(v999.getNumber());
    assertThat(v888).isLessThan(v999);
    assertThat(v999).isGreaterThan(v888);
    assertThat(v888.compareVersion(v999)).isEqualTo(VersionComparisonResult.LESS);

    // different values with the same (clamped) number must not compare equal
    VersionIdentifier versionTestIdentifierA = VersionIdentifier.ofRequired("0.0.123456789012345678901234567890");
    VersionIdentifier versionTestIdentifierB = VersionIdentifier.ofRequired("0.0.123456789012345678901234567891");
    assertThat(versionTestIdentifierB.compareVersion(versionTestIdentifierA)).isEqualTo(VersionComparisonResult.GREATER);
    assertThat(versionTestIdentifierA.compareVersion(versionTestIdentifierB)).isEqualTo(VersionComparisonResult.LESS);
  }

  /**
   * Test the digit based comparison used for overflowed values: leading zeros are ignored and the numeric value decides.
   */
  @Test
  void testCompareOverflowedDigits() {

    // different significant length after stripping leading zeros -> the longer (more digits) is larger
    assertThat(VersionSegment.of("8888888888").compareVersion(VersionSegment.of("99999999999")).isLess()).isTrue();
    // same length -> the first differing digit (left to right) decides
    assertThat(VersionSegment.of("899999999999").compareVersion(VersionSegment.of("999999999999")).isLess()).isTrue();
    // leading zeros are stripped before comparing ("0199...9" is smaller than "299...9" even though it is longer)
    assertThat(VersionSegment.of("01999999999999").compareVersion(VersionSegment.of("29999999999999")).isLess()).isTrue();
    // same numeric value (clamped) but different digits -> safe EQUAL
    assertThat(VersionSegment.of("12345678910123").compareVersion(VersionSegment.of("012345678910123"))).isEqualTo(VersionComparisonResult.EQUAL);
  }

  /**
   * Test that versions whose digits overflow an {@code int} are still parsed and ranked correctly.
   */
  @Test
  void testOverflowVersionsAreKept() {

    VersionIdentifier v888 = VersionIdentifier.ofRequired("0.0.8888888888");
    VersionIdentifier v899 = VersionIdentifier.ofRequired("0.0.899999999999");
    VersionIdentifier v999 = VersionIdentifier.ofRequired("0.0.99999999999999");
    VersionIdentifier v610 = VersionIdentifier.ofRequired("0.61.0");

    // the full digits are kept so the version can be rebuilt as a String
    assertThat(v899.toString()).isEqualTo("0.0.899999999999");
    // a very large (overflowing) version is still ranked below a normal larger version
    assertThat(v888.isLess(v610)).isTrue();
    // the overflowing versions keep a consistent order among themselves
    assertThat(v888.isLess(v899)).isTrue();
    assertThat(v899.isLess(v999)).isTrue();
  }

}
