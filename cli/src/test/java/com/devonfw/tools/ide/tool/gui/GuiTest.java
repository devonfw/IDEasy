package com.devonfw.tools.ide.tool.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.WindowsSymlinkTestHelper;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Gui}.
 */
class GuiTest extends AbstractIdeContextTest {

  /** Version in effect before the tests so it can be restored by {@link #restoreVersion()}. */
  private final String originalVersion = IdeVersion.getVersionString();

  @TempDir
  Path tempDir;

  @AfterEach
  void restoreVersion() {

    // the individual tests change the version via mock helpers; restore the pristine value for subsequent tests
    IdeVersion.setMockVersionForTesting(this.originalVersion);
  }

  private ToolInstallation newJavaInstallation(IdeTestContext context) {

    Path binDir = context.getSoftwarePath().resolve("java/bin");
    context.getFileAccess().mkdirs(binDir);
    Path javaExecutable = binDir.resolve("java");
    try {
      Files.createFile(javaExecutable);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return new ToolInstallation(binDir.getParent(), binDir.getParent(), binDir, VersionIdentifier.of("25.0.0"), false);
  }

  @Test
  void testGetGuiExecutableOnMacCreatesAppBundleWithSymlinkNamedIDEasy() throws Exception {

    // arrange
    WindowsSymlinkTestHelper.assumeSymlinksSupported();
    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    context.setSystemInfo(SystemInfoMock.MAC_ARM64);
    ToolInstallation javaInstallation = newJavaInstallation(context);
    Gui gui = new Gui(context);

    // act
    String executable = gui.getGuiExecutable(javaInstallation);

    // assert
    Path contentsDir = context.getTempPath().resolve("IDEasy.app").resolve("Contents");
    Path launcher = contentsDir.resolve("MacOS").resolve("IDEasy");
    assertThat(executable).isEqualTo(launcher.toString());
    assertThat(Files.isSymbolicLink(launcher)).isTrue();
    assertThat(launcher.toRealPath()).isEqualTo(javaInstallation.binDir().resolve("java").toRealPath());
    Path infoPlist = contentsDir.resolve("Info.plist");
    assertThat(infoPlist).exists();
    assertThat(Files.readString(infoPlist)).contains("<key>CFBundleName</key>", "<string>IDEasy</string>",
        "<key>CFBundleIdentifier</key>");
  }

  @Test
  void testGetGuiExecutableOnWindowsReturnsPlainJava() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    ToolInstallation javaInstallation = newJavaInstallation(context);
    Gui gui = new Gui(context);

    // act
    String executable = gui.getGuiExecutable(javaInstallation);

    // assert
    assertThat(executable).isEqualTo("java");
  }

  @Test
  void testGetGuiExecutableOnLinuxReturnsPlainJava() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    ToolInstallation javaInstallation = newJavaInstallation(context);
    Gui gui = new Gui(context);

    // act
    String executable = gui.getGuiExecutable(javaInstallation);

    // assert
    assertThat(executable).isEqualTo("java");
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
    List<String> args = Gui.buildMvnArgs(installationPath, installationPath.resolve("gui/pom.xml"), "java");

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
    List<String> args = Gui.buildMvnArgs(installationPath, installationPath.resolve("gui/pom.xml"), "java");

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
    List<String> args = Gui.buildMvnArgs(installationPath, installationPath.resolve("gui/pom.xml"), "java");

    // assert
    assertThat(args).doesNotContain("-U");
    assertThat(args).doesNotContain(m2RepoFlag);
    assertThat(args).doesNotContain("-o");
  }

  @Test
  void testBuildMvnArgsUsesGivenExecutable() {

    // arrange
    Path installationPath = this.tempDir;
    IdeVersion.setMockVersionForTesting("2026.08.001");

    // act
    List<String> args = Gui.buildMvnArgs(installationPath, installationPath.resolve("gui/pom.xml"), "/tmp/IDEasy.app/Contents/MacOS/IDEasy");

    // assert
    assertThat(args).contains("-Dexec.executable=/tmp/IDEasy.app/Contents/MacOS/IDEasy");
  }

}
