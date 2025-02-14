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
    checkVersionRange("[1, )", "1", null, BoundaryType.RIGHT_OPEN);
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
   * Test of {@link VersionRange#contains(VersionIdentifier)}.
   */
  @Test
  public void testContains() {

    // assert
    checkContains("1.2,3.4", "1.2");
    checkContains("1.2,3.4", "2");
    checkContains("1.2,3.4", "3.4");

    checkContainsNot("(1.2,3.4)", "1.2");
    checkContains("(1.2,3.4)", "1.2.1");
    checkContains("(1.2,3.4)", "2");
    checkContains("(1.2,3.4)", "3.3.9");
    checkContainsNot("(1.2,3.4)", "3.4");

    checkContains("[11,22]", "11");
    checkContains("[11,22]", "22");
    checkContainsNot("[11,22]", "22.1");
    checkContainsNot("[11,22]", "22_1");
    checkContainsNot("1.2,3.4", "1.1");
    checkContainsNot("1.2,3.4", "3.4.1");

    checkContainsNot("(1.2,3.4)", "1.2");
    checkContainsNot("(1.2,3.4)", "3.4");

    checkContainsNot("(11,22)", "10*");
    checkContains("(11,22)", "11*");
    checkContains("(11,22)", "11.0*");
    checkContains("(11,22)", "21.99*");
    checkContainsNot("(11,22)", "22*");
    checkContainsNot("(11,22)", "22.*");
    checkContainsNot("(11,22)", "23*");
    checkContains("(11,22)", "21.99*");
    checkContains("[22,)", "*");
  }

  private void checkContains(String range, String version) {

    checkContains(range, version, true);
  }

  private void checkContainsNot(String range, String version) {

    checkContains(range, version, false);
  }

  private void checkContains(String range, String version, boolean contains) {

    assertThat(VersionRange.of(range).contains(VersionIdentifier.of(version))).as("%s contains %s", range, version).isEqualTo(contains);
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
