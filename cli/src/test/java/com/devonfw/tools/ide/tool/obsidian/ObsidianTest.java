package com.devonfw.tools.ide.tool.obsidian;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelperMock;
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

  /**
   * Test that an existing installation on Windows is found via the registry, even though Obsidian is not in the PATH.
   *
   * @param installDir the {@link Path} to the installation directory of Obsidian.
   */
  @Test
  void testGetInstallationPathFromWindowsRegistry(@TempDir Path installDir) {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelperMock windowsHelper = (WindowsHelperMock) context.getWindowsHelper();
    windowsHelper.setAppInstallationFromRegistry("Obsidian", new WindowsAppInstallation("1.8.9", null, null, installDir.toString()));
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act
    Path installationPath = obsidian.getInstallationPath("obsidian", null);

    // assert
    assertThat(installationPath).isEqualTo(installDir);
  }

  private Obsidian obsidianOnWindowsWithRegistry(WindowsAppInstallation installation) {

    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelperMock windowsHelper = (WindowsHelperMock) context.getWindowsHelper();
    windowsHelper.setAppInstallationFromRegistry("Obsidian", installation);
    return context.getCommandletManager().getCommandlet(Obsidian.class);
  }

  /**
   * Test that the installation folder is derived from the UninstallString if the registry has no InstallLocation, also if the path contains spaces.
   *
   * @param tempDir the temporary folder.
   * @throws IOException on error.
   */
  @Test
  void testGetInstallationPathFallsBackToUninstallString(@TempDir Path tempDir) throws IOException {

    // arrange
    Path installDir = Files.createDirectories(tempDir.resolve("First Last").resolve("Obsidian"));
    String uninstallString = "\"" + installDir.resolve("Uninstall Obsidian.exe") + "\" /currentuser";
    Obsidian obsidian = obsidianOnWindowsWithRegistry(new WindowsAppInstallation("1.8.9", null, uninstallString, null));

    // act
    Path installationPath = obsidian.getInstallationPath("obsidian", null);

    // assert
    assertThat(installationPath).isEqualTo(installDir);
  }

  /**
   * Test that the installation folder is derived from the DisplayIcon if neither InstallLocation nor a usable UninstallString exist.
   *
   * @param tempDir the temporary folder.
   * @throws IOException on error.
   */
  @Test
  void testGetInstallationPathFallsBackToDisplayIcon(@TempDir Path tempDir) throws IOException {

    // arrange
    Path installDir = Files.createDirectories(tempDir.resolve("First Last").resolve("Obsidian"));
    String icon = installDir.resolve("Obsidian.exe") + ",0";
    Obsidian obsidian = obsidianOnWindowsWithRegistry(new WindowsAppInstallation("1.8.9", icon, null, null));

    // act
    Path installationPath = obsidian.getInstallationPath("obsidian", null);

    // assert
    assertThat(installationPath).isEqualTo(installDir);
  }

  /**
   * Test that an existing InstallLocation is preferred over the folders derived from the other registry values.
   *
   * @param tempDir the temporary folder.
   * @throws IOException on error.
   */
  @Test
  void testGetInstallationPathPrefersInstallLocation(@TempDir Path tempDir) throws IOException {

    // arrange
    Path installDir = Files.createDirectories(tempDir.resolve("location"));
    Path otherDir = Files.createDirectories(tempDir.resolve("other"));
    String uninstallString = "\"" + otherDir.resolve("Uninstall Obsidian.exe") + "\" /currentuser";
    Obsidian obsidian = obsidianOnWindowsWithRegistry(new WindowsAppInstallation("1.8.9", null, uninstallString, installDir.toString()));

    // act
    Path installationPath = obsidian.getInstallationPath("obsidian", null);

    // assert
    assertThat(installationPath).isEqualTo(installDir);
  }

  /**
   * Test that no installation is reported if none of the registry values points to an existing folder.
   *
   * @param tempDir the temporary folder.
   */
  @Test
  void testGetInstallationPathIsNullWithoutUsableFolder(@TempDir Path tempDir) {

    // arrange
    String missing = tempDir.resolve("does-not-exist").toString();
    Obsidian obsidian = obsidianOnWindowsWithRegistry(
        new WindowsAppInstallation("1.8.9", missing + "/Obsidian.exe,0", "\"" + missing + "/Uninstall Obsidian.exe\" /currentuser", missing));

    // act
    Path installationPath = obsidian.getInstallationPath("obsidian", null);

    // assert
    assertThat(installationPath).isNull();
  }

  /**
   * Test of {@link Obsidian#getFolderOfExecutable(String)} with the formats found in the registry.
   */
  @Test
  void testGetFolderOfExecutable() {

    assertThat(Obsidian.getFolderOfExecutable("\"C:/Users/First Last/App/Uninstall Obsidian.exe\" /currentuser")).isEqualTo(Path.of("C:/Users/First Last/App"));
    assertThat(Obsidian.getFolderOfExecutable("\"C:/Users/First Last/App/Obsidian.exe\",0")).isEqualTo(Path.of("C:/Users/First Last/App"));
    assertThat(Obsidian.getFolderOfExecutable("C:/Users/First Last/App/Obsidian.exe,0")).isEqualTo(Path.of("C:/Users/First Last/App"));
    assertThat(Obsidian.getFolderOfExecutable("C:/Users/First Last/App/UNINSTALL.EXE /S")).isEqualTo(Path.of("C:/Users/First Last/App"));
    assertThat(Obsidian.getFolderOfExecutable("\"unterminated.exe")).isNull();
    assertThat(Obsidian.getFolderOfExecutable("")).isNull();
    assertThat(Obsidian.getFolderOfExecutable(null)).isNull();
  }
}
