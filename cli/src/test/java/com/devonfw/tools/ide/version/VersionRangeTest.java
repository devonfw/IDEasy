package com.devonfw.tools.ide.version;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionRangeTest extends Assertions {
  @Test
  void testEquals() {

    assertThat(VersionRange.of("1.2>")).isEqualTo(VersionRange.of("1.2>"));
    assertThat(VersionRange.of("1.2>3")).isEqualTo(VersionRange.of("1.2>3"));
    assertThat(VersionRange.of(">3")).isEqualTo(VersionRange.of(">3"));
    assertThat(VersionRange.of(">")).isEqualTo(VersionRange.of(">"));
    assertThat(VersionRange.of("8u302b08>11.0.14_9")).isEqualTo(VersionRange.of("8u302b08>11.0.14_9"));

    assertThat(VersionRange.of("1>")).isNotEqualTo(null);
    assertThat(VersionRange.of("1.2>")).isNotEqualTo(VersionRange.of("1>"));
    assertThat(VersionRange.of("1.2>3")).isNotEqualTo(VersionRange.of("1.2>"));
    assertThat(VersionRange.of("1.2>3")).isNotEqualTo(VersionRange.of(">3"));
    assertThat(VersionRange.of("1.2>")).isNotEqualTo(VersionRange.of("1.2>3"));
    assertThat(VersionRange.of(">3")).isNotEqualTo(VersionRange.of("1.2>3"));
    assertThat(VersionRange.of(">3")).isNotEqualTo(VersionRange.of(">"));
    assertThat(VersionRange.of(">")).isNotEqualTo(VersionRange.of(">3"));
    assertThat(VersionRange.of("8u302b08>11.0.14_9")).isNotEqualTo(VersionRange.of("8u302b08>11.0.15_9"));
    assertThat(VersionRange.of("8u302b08>11.0.14_9")).isNotEqualTo(VersionRange.of("8u302b08>11.0.14_0"));

  }
}
