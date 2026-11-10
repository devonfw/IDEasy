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
   * Recording the CVE check for one version of a tool must not make another version of the same tool look already checked, and vice versa. A tool can be
   * installed and CVE-checked in several versions within one process (e.g. as a top-level tool and as an out-of-range dependency resolving to a different
   * version), so each tool+edition+version has to be remembered independently.
   * See <a href="https://github.com/devonfw/IDEasy/issues/2827">#2827</a>.
   */
  @Test
  void testCveCheckIsRememberedPerVersion() {

    // arrange
    ToolInstallTracker tracker = new ToolInstallTracker();
    ToolEdition java = new ToolEdition("java", "java");

    // act
    tracker.recordCveChecked(java, VersionIdentifier.of("21.0.9_10"));
    tracker.recordCveChecked(java, VersionIdentifier.of("17.0.6"));

    // assert - both versions are still remembered after the second one was recorded
    assertThat(tracker.isCveChecked(java, VersionIdentifier.of("21.0.9_10"))).isTrue();
    assertThat(tracker.isCveChecked(java, VersionIdentifier.of("17.0.6"))).isTrue();
    // a version that was never recorded is not
    assertThat(tracker.isCveChecked(java, VersionIdentifier.of("25.0.1"))).isFalse();
  }

  /**
   * Like {@link #testCveCheckIsRememberedPerVersion()} but for the installed state: installing several versions of the same tool must each be remembered, so a
   * redundant install is skipped per version no matter how many versions of the tool were installed.
   */
  @Test
  void testInstalledIsRememberedPerVersion() {

    // arrange
    ToolInstallTracker tracker = new ToolInstallTracker();

    // act
    tracker.recordInstalled(editionAndVersion("java", "java", "21.0.9_10"));
    tracker.recordInstalled(editionAndVersion("java", "java", "17.0.6"));

    // assert
    assertThat(tracker.isInstalled(editionAndVersion("java", "java", "21.0.9_10"))).isTrue();
    assertThat(tracker.isInstalled(editionAndVersion("java", "java", "17.0.6"))).isTrue();
    assertThat(tracker.isInstalled(editionAndVersion("java", "java", "25.0.1"))).isFalse();
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
