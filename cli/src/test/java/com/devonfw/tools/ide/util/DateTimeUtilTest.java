package com.devonfw.tools.ide.util;

import java.time.Duration;
import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link DateTimeUtil}.
 */
public class DateTimeUtilTest extends Assertions {

  private static final Instant INSTANT_1999_12_31 = Instant.parse("1999-12-31T23:59:59.987654321Z");

  private static final Instant INSTANT_2000_01_02 = Instant.parse("2000-01-02T23:59:59.987654321Z");

  private static final Duration TWO_DAYS = Duration.ofDays(2);

  /**
   * Test of {@link DateTimeUtil#isAfter(Instant, Instant)}.
   */
  @Test
  public void testIsAfter() {

    assertThat(DateTimeUtil.isAfter(INSTANT_2000_01_02, INSTANT_1999_12_31)).isTrue();
    assertThat(DateTimeUtil.isAfter(INSTANT_2000_01_02, INSTANT_2000_01_02)).isFalse();
    assertThat(DateTimeUtil.isAfter(INSTANT_1999_12_31, INSTANT_2000_01_02)).isFalse();
  }

  /**
   * Test of {@link DateTimeUtil#isAfter(Instant, Instant)}.
   */
  @Test
  public void testIsBefore() {

    assertThat(DateTimeUtil.isBefore(INSTANT_2000_01_02, INSTANT_1999_12_31)).isFalse();
    assertThat(DateTimeUtil.isBefore(INSTANT_2000_01_02, INSTANT_2000_01_02)).isFalse();
    assertThat(DateTimeUtil.isBefore(INSTANT_1999_12_31, INSTANT_2000_01_02)).isTrue();
  }

  /**
   * Test of {@link DateTimeUtil#compareDuration(Instant, Instant, java.time.Duration)}.
   */
  @Test
  public void testCompareDuration() {

    assertThat(DateTimeUtil.compareDuration(INSTANT_1999_12_31, INSTANT_2000_01_02, TWO_DAYS)).isEqualTo(0);
    // 1 nano second longer
    assertThat(
        DateTimeUtil.compareDuration(INSTANT_1999_12_31, Instant.parse("2000-01-02T23:59:59.987654322Z"), TWO_DAYS))
            .isEqualTo(1);
    // 1 nano second shorter
    assertThat(
        DateTimeUtil.compareDuration(INSTANT_1999_12_31, Instant.parse("2000-01-02T23:59:59.987654320Z"), TWO_DAYS))
            .isEqualTo(-1);
  }

}
