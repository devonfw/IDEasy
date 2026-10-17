package com.devonfw.tools.ide.tool.obsidian;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Obsidian}.
 */
class ObsidianTest extends AbstractIdeContextTest {

  private static final String PROJECT_OBSIDIAN = "obsidian";

  private static final String OBSIDIAN_VERSION = "1.12.7";

  /**
   * Test that the {@link Obsidian} commandlet is registered and properly classified.
   */
  @Test
  void testObsidianCommandletIsRegistered() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // assert
    assertThat(obsidian).isNotNull();
    assertThat(obsidian.getName()).isEqualTo("obsidian");
    assertThat(obsidian.getTags()).containsExactly(Tag.MARK_DOWN);
  }

  /**
   * Test that the Windows download is not extracted since it is an installer executable that has to be started.
   */
  @Test
  void testIsExtractIsFalseOnWindows() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act + assert
    assertThat(obsidian.isExtract()).isFalse();
  }

  /**
   * Test that the macOS download is extracted since the *.app has to be taken out of the DMG image.
   */
  @Test
  void testIsExtractIsTrueOnMac() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act + assert
    assertThat(obsidian.isExtract()).isTrue();
  }

  /**
   * Test that the Linux download is extracted since it is a tar.gz archive.
   */
  @Test
  void testIsExtractIsTrueOnLinux() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act + assert
    assertThat(obsidian.isExtract()).isTrue();
  }

  /**
   * Test that the name used to find an existing installation in the Windows registry is not the lower-case tool name.
   */
  @Test
  void testGetWindowsRegistryAppName() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act + assert
    assertThat(obsidian.getWindowsRegistryAppName()).isEqualTo("Obsidian");
  }

  /**
   * Test that the installation on Linux persists the extracted archive in the user home and records the installed version.
   */
  @Test
  void testInstallOnLinuxPersistsInstallation() {

    // arrange
    IdeTestContext context = newContext(PROJECT_OBSIDIAN);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Obsidian obsidian = new Obsidian(context);

    // act
    obsidian.install();

    // assert
    Path installationDir = getLinuxInstallationDir(context);
    assertThat(installationDir.resolve("obsidian")).exists();
    assertThat(installationDir.resolve(IdeContext.FILE_SOFTWARE_VERSION)).hasContent(OBSIDIAN_VERSION);
    assertThat(obsidian.getInstalledVersion()).isEqualTo(VersionIdentifier.of(OBSIDIAN_VERSION));
  }

  /**
   * Test that the installation on macOS persists the *.app bundle in the Applications folder of the user and detects its version from the bundle.
   */
  @Test
  void testInstallOnMacPersistsAppBundle() {

    // arrange
    IdeTestContext context = newContext(PROJECT_OBSIDIAN);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Obsidian obsidian = new Obsidian(context);

    // act
    obsidian.install();

    // assert
    Path appBundle = getMacAppBundle(context);
    assertThat(appBundle.resolve("Contents").resolve("MacOS").resolve("Obsidian")).exists();
    assertThat(obsidian.getInstalledVersion()).isEqualTo(VersionIdentifier.of(OBSIDIAN_VERSION));
  }

  /**
   * Test that a persisted installation is detected so that a second installation is skipped.
   */
  @Test
  void testInstallIsSkippedIfAlreadyInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_OBSIDIAN);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    new Obsidian(context).install();
    Obsidian obsidian = new Obsidian(context);

    // act
    obsidian.install(false);

    // assert
    assertThat(context).logAtInfo().hasMessage("Version " + OBSIDIAN_VERSION + " of tool obsidian is already installed");
  }

  /**
   * Test that the uninstallation on Linux removes the persisted installation.
   */
  @Test
  void testUninstallOnLinuxRemovesInstallation() {

    // arrange
    IdeTestContext context = newContext(PROJECT_OBSIDIAN);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    new Obsidian(context).install();
    Obsidian obsidian = new Obsidian(context);

    // act
    obsidian.uninstall();

    // assert
    assertThat(getLinuxInstallationDir(context)).doesNotExist();
    assertThat(obsidian.getInstalledVersion()).isNull();
  }

  private static Path getLinuxInstallationDir(IdeTestContext context) {

    return context.getUserHome().resolve(".local").resolve("share").resolve("obsidian");
  }

  private static Path getMacAppBundle(IdeTestContext context) {

    return context.getUserHome().resolve("Applications").resolve("Obsidian.app");
  }
}
