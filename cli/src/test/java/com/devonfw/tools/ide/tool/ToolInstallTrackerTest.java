package com.devonfw.tools.ide.tool;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Tests for {@link ToolInstallTracker}.
 */
class ToolInstallTrackerTest extends Assertions {

  private static ToolEditionAndVersion editionAndVersion(String tool, String edition, String version) {

    return new ToolEditionAndVersion(new ToolEdition(tool, edition), VersionIdentifier.of(version));
  }

  /**
   * A recorded tool+edition+version is reported as installed, even when compared against a differently-parsed instance with the same values; a different
   * version or a different edition of the same tool is not.
   */
  @Test
  void testRecordAndIsInstalled() {

    // arrange
    ToolInstallTracker tracker = new ToolInstallTracker();
    ToolEditionAndVersion java17 = editionAndVersion("java", "java", "17.0.6");

    // act
    tracker.recordInstalled(java17);

    // assert
    assertThat(tracker.isInstalled(java17)).isTrue();
    // a different (but textually identical) version instance of the same tool/edition is also recognized
    assertThat(tracker.isInstalled(editionAndVersion("java", "java", "17.0.6"))).isTrue();
    // but a different version is not
    assertThat(tracker.isInstalled(editionAndVersion("java", "java", "21.0.9_10"))).isFalse();
    // and a different edition is not
    assertThat(tracker.isInstalled(editionAndVersion("java", "azul", "17.0.6"))).isFalse();
  }

  /**
   * An empty tracker reports nothing as installed, and recording a {@link ToolEditionAndVersion} without a resolved version is a no-op.
   */
  @Test
  void testEmptyTrackerAndUnresolvedVersion() {

    // arrange
    ToolInstallTracker tracker = new ToolInstallTracker();
    ToolEditionAndVersion java17 = editionAndVersion("java", "java", "17.0.6");

    // act + assert
    assertThat(tracker.isInstalled(java17)).isFalse();
    tracker.recordInstalled(null);
    tracker.recordInstalled(new ToolEditionAndVersion(new ToolEdition("java", "java"))); // no version resolved yet
    assertThat(tracker.isInstalled(java17)).isFalse();
  }
}
