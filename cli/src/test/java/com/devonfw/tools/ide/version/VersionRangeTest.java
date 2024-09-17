package com.devonfw.tools.ide.version;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link VersionRange}.
 */
public class VersionRangeTest extends Assertions {

  /** Test of {@link VersionRange#of(String)}. */
  @Test
  public void testOf() {

    checkVersionRange("1.2,3", "1.2", "3", BoundaryType.CLOSED);
    checkVersionRange("[1.2,3]", "1.2", "3", BoundaryType.CLOSED);
    checkVersionRange("1,)", "1", null, BoundaryType.RIGHT_OPEN);
    checkVersionRange("[1,)", "1", null, BoundaryType.RIGHT_OPEN);
    checkVersionRange("(1.2,3.4", "1.2", "3.4", BoundaryType.LEFT_OPEN);
    checkVersionRange("(1.2,3.4]", "1.2", "3.4", BoundaryType.LEFT_OPEN);
    checkVersionRange("1.2,3.4)", "1.2", "3.4", BoundaryType.RIGHT_OPEN);
    checkVersionRange("[1.2,3.4)", "1.2", "3.4", BoundaryType.RIGHT_OPEN);
    checkVersionRange("(,)", null, null, BoundaryType.OPEN);
    checkVersionRange(",", null, null, BoundaryType.OPEN);
  }

  // arrange
  private VersionRange checkVersionRange(String range, String min, String max, BoundaryType boundaryType) {

    // act
    VersionRange versionRange = VersionRange.of(range);

    // assert
    assertThat(versionRange.getMin()).isEqualTo(VersionIdentifier.of(min));
    assertThat(versionRange.getMax()).isEqualTo(VersionIdentifier.of(max));
    assertThat(versionRange.getBoundaryType()).isEqualTo(boundaryType);
    if ((min == null) && (max == null)) {
      assertThat(versionRange).isSameAs(VersionRange.UNBOUNDED);
    }
    return versionRange;
  }

  /** Test of {@link VersionRange#toString()}. */
  @Test
  public void testToString() {

    assertThat(VersionRange.of("1.2,3").toString()).isEqualTo("[1.2,3]");
    assertThat(VersionRange.of("1,)").toString()).isEqualTo("[1,)");
    assertThat(VersionRange.of("(1.2,3.4]").toString()).isEqualTo("(1.2,3.4]");
    assertThat(VersionRange.of(",").toString()).isEqualTo("(,)");
  }

  /** Test of {@link VersionRange#equals(Object)}. */
  @Test
  public void testEquals() {

    // assert
    // equals
    assertThat(VersionRange.of("1.2,")).isEqualTo(VersionRange.of("1.2,"));
    assertThat(VersionRange.of("(1.2,")).isEqualTo(VersionRange.of("(1.2,)"));
    assertThat(VersionRange.of("1.2,3")).isEqualTo(VersionRange.of("1.2,3"));
    assertThat(VersionRange.of("[1.2,3")).isEqualTo(VersionRange.of("1.2,3]"));
    assertThat(VersionRange.of(",3)")).isEqualTo(VersionRange.of(",3)"));
    assertThat(VersionRange.of(",")).isEqualTo(VersionRange.of("(,)"));
    assertThat(VersionRange.of("8u302b08,11.0.14_9")).isEqualTo(VersionRange.of("8u302b08,11.0.14_9"));
    // not equals
    assertThat(VersionRange.of("1,")).isNotEqualTo(null);
    assertThat(VersionRange.of("1.2,")).isNotEqualTo(VersionRange.of("1,"));
    assertThat(VersionRange.of("1.2,3")).isNotEqualTo(VersionRange.of("1.2,"));
    assertThat(VersionRange.of("(1.2,3")).isNotEqualTo(VersionRange.of("1.2.3,"));
    assertThat(VersionRange.of("1.2,3")).isNotEqualTo(VersionRange.of(",3"));
    assertThat(VersionRange.of("[1.2,")).isNotEqualTo(VersionRange.of("[1.2,3"));
    assertThat(VersionRange.of(",3")).isNotEqualTo(VersionRange.of("1.2,3"));
    assertThat(VersionRange.of(",3")).isNotEqualTo(VersionRange.of(","));
    assertThat(VersionRange.of(",")).isNotEqualTo(VersionRange.of(",3"));
    assertThat(VersionRange.of("8u302b08,11.0.14_9")).isNotEqualTo(VersionRange.of("(8u302b08,11.0.14_9)"));
    assertThat(VersionRange.of("8u302b08,11.0.14_9")).isNotEqualTo(VersionRange.of("8u302b08,11.0.15_9"));
    assertThat(VersionRange.of("8u302b08,11.0.14_9")).isNotEqualTo(VersionRange.of("8u302b08,11.0.14_0"));
  }

  /**
   * Test of {@link VersionRange#contains(VersionIdentifier)} and testing if a {@link VersionIdentifier version} is contained in the {@link VersionRange}.
   */
  @Test
  public void testContains() {

    // assert
    assertThat(VersionRange.of("1.2,3.4").contains(VersionIdentifier.of("1.2"))).isTrue();
    assertThat(VersionRange.of("1.2,3.4").contains(VersionIdentifier.of("2"))).isTrue();
    assertThat(VersionRange.of("1.2,3.4").contains(VersionIdentifier.of("3.4"))).isTrue();

    assertThat(VersionRange.of("(1.2,3.4)").contains(VersionIdentifier.of("1.2"))).isFalse();
    assertThat(VersionRange.of("(1.2,3.4)").contains(VersionIdentifier.of("1.2.1"))).isTrue();
    assertThat(VersionRange.of("(1.2,3.4)").contains(VersionIdentifier.of("2"))).isTrue();
    assertThat(VersionRange.of("(1.2,3.4)").contains(VersionIdentifier.of("3.3.9"))).isTrue();
    assertThat(VersionRange.of("(1.2,3.4)").contains(VersionIdentifier.of("3.4"))).isFalse();

    assertThat(VersionRange.of("[11,22]").contains(VersionIdentifier.of("11"))).isTrue();
    assertThat(VersionRange.of("[11,22]").contains(VersionIdentifier.of("22"))).isTrue();
    assertThat(VersionRange.of("[11,22]").contains(VersionIdentifier.of("22.1"))).isFalse();
    assertThat(VersionRange.of("[11,22]").contains(VersionIdentifier.of("22_1"))).isFalse();
  }

  /**
   * Test of {@link VersionRange#contains(VersionIdentifier)} and testing if a {@link VersionIdentifier version} is not contained in the {@link VersionRange}.
   */
  @Test
  public void testNotContains() {

    // assert
    assertThat(VersionRange.of("1.2,3.4").contains(VersionIdentifier.of("1.1"))).isFalse();
    assertThat(VersionRange.of("1.2,3.4").contains(VersionIdentifier.of("3.4.1"))).isFalse();

    assertThat(VersionRange.of("(1.2,3.4)").contains(VersionIdentifier.of("1.2"))).isFalse();
    assertThat(VersionRange.of("(1.2,3.4)").contains(VersionIdentifier.of("3.4"))).isFalse();
  }

  /** Test of {@link VersionRange#compareTo(VersionRange)} and testing if versions are compared to be the same. */
  @Test
  public void testCompareToIsSame() {

    // assert
    assertThat(VersionRange.of("1.2,3").compareTo(VersionRange.of("1.2,3"))).isEqualTo(0);
    assertThat(VersionRange.of("(1.2,3").compareTo(VersionRange.of("(1.2,3"))).isEqualTo(0);
    assertThat(VersionRange.of("[1.2,3]").compareTo(VersionRange.of("[1.2,4)"))).isEqualTo(0);
  }

  /** Test of {@link VersionRange#compareTo(VersionRange)} and testing if first version is smaller than second. */
  @Test
  public void testCompareToIsSmaller() {

    // assert
    assertThat(VersionRange.of("1.1.2,3").compareTo(VersionRange.of("1.2,3"))).isEqualTo(-1);
    assertThat(VersionRange.of("[1.2,3").compareTo(VersionRange.of("(1.2,4"))).isEqualTo(-1);
  }

  /** Test of {@link VersionRange#compareTo(VersionRange)} and testing if first version is larger than second. */
  @Test
  public void testCompareToIsLarger() {

    // assert
    assertThat(VersionRange.of("1.2.1,3").compareTo(VersionRange.of("1.2,3"))).isEqualTo(1);
    assertThat(VersionRange.of("(1.2,3").compareTo(VersionRange.of("1.2,4"))).isEqualTo(1);
  }

  /** Test of {@link VersionRange#of(String)} with illegal syntax. */
  @Test
  public void testIllegalSyntax() {

    checkIllegalRange("[,)");
    checkIllegalRange("(,]");
    checkIllegalRange("[,]");
    checkIllegalRange("[,1.0)");
    checkIllegalRange("(1.0,]");
    checkIllegalRange("(1.1,1.0)");
  }

  private void checkIllegalRange(String range) {

    try {
      VersionRange.of(range);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(range);
    }
  }
}
