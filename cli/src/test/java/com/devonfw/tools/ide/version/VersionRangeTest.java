package com.devonfw.tools.ide.version;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link VersionRange}.
 */
public class VersionRangeTest extends Assertions {

  /**
   * Test of {@link VersionRange#of(String)}.
   */
  @Test
  public void testOf() {

    // arrange
    String v1String = "1.2>3";
    String v2String = "1>)";
    String v3String = "(1.2>3.4]";

    // act
    VersionRange v1 = VersionRange.of(v1String);
    VersionRange v2 = VersionRange.of(v2String);
    VersionRange v3 = VersionRange.of(v3String);

    // assert
    // v1
    assertThat(v1.getMin()).isEqualTo(VersionIdentifier.of("1.2"));
    assertThat(v1.getMax()).isEqualTo(VersionIdentifier.of("3"));
    assertThat(v1.isLeftExclusive()).isFalse();
    assertThat(v1.isRightExclusive()).isFalse();
    // v2
    assertThat(v2.getMin()).isEqualTo(VersionIdentifier.of("1"));
    assertThat(v2.getMax()).isEqualTo(null);
    assertThat(v2.isLeftExclusive()).isFalse();
    assertThat(v2.isRightExclusive()).isTrue();
    // v3
    assertThat(v3.getMin()).isEqualTo(VersionIdentifier.of("1.2"));
    assertThat(v3.getMax()).isEqualTo(VersionIdentifier.of("3.4"));
    assertThat(v3.isLeftExclusive()).isTrue();
    assertThat(v3.isRightExclusive()).isFalse();
  }

  /**
   * Test of {@link VersionRange#toString()}.
   */
  @Test
  public void testToString() {

    assertThat(VersionRange.of("1.2>3").toString()).isEqualTo("[1.2>3]");
    assertThat(VersionRange.of("1>)").toString()).isEqualTo("[1>)");
    assertThat(VersionRange.of("(1.2>3.4]").toString()).isEqualTo("(1.2>3.4]");
  }

  /**
   * Test of {@link VersionRange#equals(Object)}.
   */
  @Test
  public void testEquals() {

    // assert
    // equals
    assertThat(VersionRange.of("1.2>")).isEqualTo(VersionRange.of("1.2>"));
    assertThat(VersionRange.of("(1.2>")).isEqualTo(VersionRange.of("(1.2>)"));
    assertThat(VersionRange.of("1.2>3")).isEqualTo(VersionRange.of("1.2>3"));
    assertThat(VersionRange.of("[1.2>3")).isEqualTo(VersionRange.of("1.2>3]"));
    assertThat(VersionRange.of(">3)")).isEqualTo(VersionRange.of(">3)"));
    assertThat(VersionRange.of(">")).isEqualTo(VersionRange.of(">"));
    assertThat(VersionRange.of("[>)")).isEqualTo(VersionRange.of("(>]"));
    assertThat(VersionRange.of("8u302b08>11.0.14_9")).isEqualTo(VersionRange.of("8u302b08>11.0.14_9"));
    // not equals
    assertThat(VersionRange.of("1>")).isNotEqualTo(null);
    assertThat(VersionRange.of("1.2>")).isNotEqualTo(VersionRange.of("1>"));
    assertThat(VersionRange.of("1.2>3")).isNotEqualTo(VersionRange.of("1.2>"));
    assertThat(VersionRange.of("(1.2>3")).isNotEqualTo(VersionRange.of("1.2.3>"));
    assertThat(VersionRange.of("1.2>3")).isNotEqualTo(VersionRange.of(">3"));
    assertThat(VersionRange.of("[1.2>")).isNotEqualTo(VersionRange.of("[1.2>3"));
    assertThat(VersionRange.of(">3")).isNotEqualTo(VersionRange.of("1.2>3"));
    assertThat(VersionRange.of(">3")).isNotEqualTo(VersionRange.of(">"));
    assertThat(VersionRange.of(">")).isNotEqualTo(VersionRange.of(">3"));
    assertThat(VersionRange.of("8u302b08>11.0.14_9")).isNotEqualTo(VersionRange.of("(8u302b08>11.0.14_9)"));
    assertThat(VersionRange.of("8u302b08>11.0.14_9")).isNotEqualTo(VersionRange.of("8u302b08>11.0.15_9"));
    assertThat(VersionRange.of("8u302b08>11.0.14_9")).isNotEqualTo(VersionRange.of("8u302b08>11.0.14_0"));
  }

  /**
   * Test of {@link VersionRange#contains(VersionIdentifier)} and testing if a {@link VersionIdentifier version} is
   * contained in the {@link VersionRange}.
   */
  @Test
  public void testContains() {

    // assert
    assertThat(VersionRange.of("1.2>3.4").contains(VersionIdentifier.of("1.2"))).isTrue();
    assertThat(VersionRange.of("1.2>3.4").contains(VersionIdentifier.of("2"))).isTrue();
    assertThat(VersionRange.of("1.2>3.4").contains(VersionIdentifier.of("3.4"))).isTrue();

    assertThat(VersionRange.of("(1.2>3.4)").contains(VersionIdentifier.of("1.2.1"))).isTrue();
    assertThat(VersionRange.of("(1.2>3.4)").contains(VersionIdentifier.of("2"))).isTrue();
    assertThat(VersionRange.of("(1.2>3.4)").contains(VersionIdentifier.of("3.3.9"))).isTrue();
  }

  /**
   * Test of {@link VersionRange#contains(VersionIdentifier)} and testing if a {@link VersionIdentifier version} is not
   * contained in the {@link VersionRange}.
   */
  @Test
  public void testNotContains() {

    // assert
    assertThat(VersionRange.of("1.2>3.4").contains(VersionIdentifier.of("1.1"))).isFalse();
    assertThat(VersionRange.of("1.2>3.4").contains(VersionIdentifier.of("3.4.1"))).isFalse();

    assertThat(VersionRange.of("(1.2>3.4)").contains(VersionIdentifier.of("1.2"))).isFalse();
    assertThat(VersionRange.of("(1.2>3.4)").contains(VersionIdentifier.of("3.4"))).isFalse();
  }

  /**
   * Test of {@link VersionRange#compareTo(VersionRange)} and testing if versions are compared to be the same.
   */
  @Test
  public void testCompareToIsSame() {

    // assert
    assertThat(VersionRange.of("1.2>3").compareTo(VersionRange.of("1.2>3"))).isEqualTo(0);
    assertThat(VersionRange.of("(1.2>3").compareTo(VersionRange.of("(1.2>3"))).isEqualTo(0);
    assertThat(VersionRange.of("[1.2>3]").compareTo(VersionRange.of("[1.2>4)"))).isEqualTo(0);
  }

  /**
   * Test of {@link VersionRange#compareTo(VersionRange)} and testing if first version is smaller than second.
   */
  @Test
  public void testCompareToIsSmaller() {

    // assert
    assertThat(VersionRange.of("1.1.2>3").compareTo(VersionRange.of("1.2>3"))).isEqualTo(-1);
    assertThat(VersionRange.of("[1.2>3").compareTo(VersionRange.of("(1.2>4"))).isEqualTo(-1);
  }

  /**
   * Test of {@link VersionRange#compareTo(VersionRange)} and testing if first version is larger than second.
   */
  @Test
  public void testCompareToIsLarger() {

    // assert
    assertThat(VersionRange.of("1.2.1>3").compareTo(VersionRange.of("1.2>3"))).isEqualTo(1);
    assertThat(VersionRange.of("(1.2>3").compareTo(VersionRange.of("1.2>4"))).isEqualTo(1);
  }
}
