package com.devonfw.tools.ide.version;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link VersionSegment}.
 */
public class VersionSegmentTest extends Assertions {

  /**
   * Test of {@link VersionSegment#ofEmpty()} and {@link VersionSegment#isEmpty()}.
   */
  @Test
  public void testEmpty() {

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
  public void testOne() {

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
  public void testDotRc2() {

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
  public void testPattern() {

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

}
