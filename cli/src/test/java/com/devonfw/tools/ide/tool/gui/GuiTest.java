package com.devonfw.tools.ide.tool.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Test of {@link Gui}.
 */
class GuiTest extends Assertions {

  /** Version in effect before the tests so it can be restored by {@link #restoreVersion()}. */
  private final String originalVersion = IdeVersion.getVersionString();

  @TempDir
  Path tempDir;

  @AfterEach
  void restoreVersion() {

    // the individual tests change the version via mock helpers; restore the pristine value for subsequent tests
    IdeVersion.setMockVersionForTesting(this.originalVersion);
  }

  /**
   * Verifies the GUI is launched from the self-contained maven repository inside the installation when the running version is a local-dev build (stamped with
   * {@link IdeVersion#LOCAL_DEV_SUFFIX}), and that the snapshot (-U) behavior is not applied in that case.
   */
  @Test
  void testBuildMvnArgsWithLocalDevBuild() {

    // arrange
    IdeVersion.setMockVersionForTesting("2026.08.002" + IdeVersion.LOCAL_DEV_SUFFIX);
    Path installationPath = this.tempDir;

    // act
    List<String> args = Gui.buildMvnArgs(installationPath, installationPath.resolve("gui/pom.xml"));

    // assert
    String m2RepoFlag = "-Dmaven.repo.local=" + installationPath.resolve(".m2");
    assertThat(args).contains(m2RepoFlag, "-o");
    assertThat(args).doesNotContain("-U");
  }

  /**
   * Verifies the snapshot (-U) behavior is kept when the installation is not a local-dev installation and the version is not stable.
   */
  @Test
  void testBuildMvnArgsWithSnapshotVersion() throws IOException {

    // arrange
    Path installationPath = Files.createDirectory(this.tempDir.resolve("snapshot"));
    IdeVersion.setSnapshotVersionForTesting();
    String m2RepoFlag = "-Dmaven.repo.local=" + installationPath.resolve(".m2");

    // act
    List<String> args = Gui.buildMvnArgs(installationPath, installationPath.resolve("gui/pom.xml"));

    // assert
    assertThat(args).contains("-U");
    assertThat(args).doesNotContain(m2RepoFlag);
    assertThat(args).doesNotContain("-o");
  }

  /**
   * Verifies neither the snapshot (-U) flag nor a self-contained repository is used for a stable version without a local-dev marker.
   */
  @Test
  void testBuildMvnArgsWithStableVersion() {

    // arrange
    Path installationPath = this.tempDir;
    IdeVersion.setMockVersionForTesting("2026.08.001");
    String m2RepoFlag = "-Dmaven.repo.local=" + installationPath.resolve(".m2");

    // act
    List<String> args = Gui.buildMvnArgs(installationPath, installationPath.resolve("gui/pom.xml"));

    // assert
    assertThat(args).doesNotContain("-U");
    assertThat(args).doesNotContain(m2RepoFlag);
    assertThat(args).doesNotContain("-o");
  }
}
