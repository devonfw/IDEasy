package com.devonfw.tools.ide.tool;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Test of {@link ToolEditionAndVersion}.
 */
class ToolEditionAndVersionTest extends Assertions {

  @Test
  void testSetResolvedVersionPopulatedOnConcretVersion() {

    // arrange
    ToolEdition te = new ToolEdition("tool", "edition");
    VersionIdentifier version = VersionIdentifier.ofRequired("1.0.0");
    // act
    ToolEditionAndVersion tev = new ToolEditionAndVersion(te, version);
    // assert
    assertThat(tev.getEdition()).isSameAs(te);
    assertThat(tev.getVersion()).isSameAs(version);
    assertThat(tev.getResolvedVersion()).isSameAs(version);
  }

  @Test
  void testSetResolvedVersionNullOnVersionPattern() {

    // arrange
    ToolEdition te = new ToolEdition("tool", "edition");
    VersionIdentifier version = VersionIdentifier.LATEST;
    // act
    ToolEditionAndVersion tev = new ToolEditionAndVersion(te, version);
    // assert
    assertThat(tev.getEdition()).isSameAs(te);
    assertThat(tev.getVersion()).isSameAs(version);
    assertThat(tev.getResolvedVersion()).isNull();
  }

  @Test
  void testSetResolvedVersionNullOnVersionRange() {

    // arrange
    ToolEdition te = new ToolEdition("tool", "edition");
    GenericVersionRange version = VersionRange.UNBOUNDED;
    // act
    ToolEditionAndVersion tev = new ToolEditionAndVersion(te, version);
    // assert
    assertThat(tev.getEdition()).isSameAs(te);
    assertThat(tev.getVersion()).isSameAs(version);
    assertThat(tev.getResolvedVersion()).isNull();
  }

  @Test
  void testEqualsAndHashCode() {

    // arrange
    ToolEdition te = new ToolEdition("tool", "edition");
    VersionIdentifier version = VersionIdentifier.ofRequired("1.0.0");
    // act
    ToolEditionAndVersion tev = new ToolEditionAndVersion(te, version);
    ToolEditionAndVersion tev2 = new ToolEditionAndVersion(te, version);
    // assert
    assertThat(tev).isEqualTo(tev2);
    assertThat(tev).hasSameHashCodeAs(tev2);
  }

}
