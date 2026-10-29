package com.devonfw.tools.ide.tool.git;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelperMock;
import com.devonfw.tools.ide.process.OutputMessage;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.tool.EditionAndVersion;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Git}.
 */
class GitTest extends AbstractIdeContextTest {

  private static final String TOOL_VERSION = "2.45.0";

  /** The version query command the package manager based version detection issues on macOS (see {@link NativePackageManager#BREW}). */
  private static final String BREW_VERSION_QUERY = "brew list --versions git";

  /** The version query command the package manager based version detection issues on Linux (see {@link NativePackageManager#APT}). */
  private static final String APT_VERSION_QUERY = "dpkg-query -W -f=${db:Status-Status}|${Version} git";

  /**
   * A {@link Git} that reports only the given native package managers as available, so a specific OS flow (apt on Linux, brew on macOS) is exercised.
   *
   * @param context the {@link IdeTestContext}.
   * @param availablePackageManagers the package managers {@code isPackageManagerAvailable} should report as available.
   * @return the {@link Git}.
   */
  private static Git git(IdeTestContext context, NativePackageManager... availablePackageManagers) {

    return new Git(context) {
      @Override
      protected boolean isPackageManagerAvailable(NativePackageManager packageManager) {
        return Arrays.asList(availablePackageManagers).contains(packageManager);
      }
    };
  }

  /**
   * Returns a mocked {@link ProcessContext} (as returned by {@code newProcess()}) whose version query yields the given single-line output, so that the package
   * manager based version detection can be simulated without running the real package manager.
   *
   * @param context the {@link IdeTestContext}.
   * @param command the command whose output should be simulated (for readability of the returned result).
   * @param output the single output line the version query should yield, or {@code null} to simulate that nothing is installed.
   * @return the mocked {@link ProcessContext}.
   */
  private ProcessContext processContextWithVersionOutput(IdeTestContext context, String command, String output) {

    ProcessContext processContext = Mockito.mock(ProcessContext.class, Mockito.RETURNS_SELF);
    List<OutputMessage> outputMessages = (output == null) ? List.of() : List.of(new OutputMessage(false, output));
    ProcessResult result = new ProcessResultImpl("bash", command, ProcessResult.SUCCESS, true, outputMessages);
    Mockito.when(processContext.run(ProcessMode.DEFAULT_CAPTURE)).thenReturn(result);
    context.setProcessContext(processContext);
    return processContext;
  }

  /**
   * Verifies that Git declares a Homebrew formula as one of its native packages, so that on macOS it can be installed via {@code brew} the same way it is
   * installed via {@code apt}/{@code zypper} on Linux.
   */
  @Test
  void testGetNativePackagesContainsBrew() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Git git = new Git(context);

    // act + assert
    assertThat(git.getNativePackages().stream().map(pkg -> pkg.getPackageManager())).contains(NativePackageManager.BREW);
  }

  /**
   * Verifies the binary name of Git.
   */
  @Test
  void testGetBinaryName() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Git git = new Git(context);

    // act + assert
    assertThat(git.getBinaryName()).isEqualTo("git");
  }

  /**
   * Verifies that the Windows registry app name of Git is {@code "Git"} (which differs from the lower case tool name).
   */
  @Test
  void testGetWindowsRegistryAppName() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Git git = new Git(context);

    // act + assert
    assertThat(git.getWindowsRegistryAppName()).isEqualTo("Git");
    assertThat(git.getWindowsRegistryAppNames()).containsEntry("git", "Git");
  }

  /**
   * Verifies that the installed Git edition and version are resolved from Homebrew ({@code brew list --versions git}) on macOS, mirroring the Linux flow.
   */
  @Test
  void testGetInstalledEditionAndVersionOnMac() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    processContextWithVersionOutput(context, BREW_VERSION_QUERY, "git " + TOOL_VERSION);
    Git git = git(context, NativePackageManager.BREW);

    // act
    EditionAndVersion editionAndVersion = git.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("git");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of(TOOL_VERSION));
  }

  /**
   * Verifies that on macOS the installed Git version is {@code null} when no git formula is installed.
   */
  @Test
  void testGetInstalledEditionAndVersionOnMacWhenNotInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    processContextWithVersionOutput(context, BREW_VERSION_QUERY, null);
    Git git = git(context, NativePackageManager.BREW);

    // act + assert
    assertThat(git.getInstalledEditionAndVersion()).isNull();
    assertThat(git.getInstalledVersion()).isNull();
  }

  /**
   * Verifies that the installed Git edition and version are resolved from apt ({@code dpkg-query}) on Linux.
   */
  @Test
  void testGetInstalledEditionAndVersionOnLinux() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    processContextWithVersionOutput(context, APT_VERSION_QUERY, "installed|" + TOOL_VERSION);
    Git git = git(context, NativePackageManager.APT);

    // act
    EditionAndVersion editionAndVersion = git.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("git");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of(TOOL_VERSION));
  }

  /**
   * Verifies that on Linux the installed Git version is {@code null} when git is not installed (the apt version query yields no installed version).
   */
  @Test
  void testGetInstalledEditionAndVersionOnLinuxWhenNotInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    processContextWithVersionOutput(context, APT_VERSION_QUERY, null);
    Git git = git(context, NativePackageManager.APT);

    // act + assert
    assertThat(git.getInstalledEditionAndVersion()).isNull();
    assertThat(git.getInstalledVersion()).isNull();
  }

  /**
   * Verifies that on Windows the installed Git edition and version are resolved from the Windows registry using the app name {@code Git}.
   */
  @Test
  void testGetInstalledEditionAndVersionOnWindows() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    helper.setAppInstallationFromRegistry("Git", new WindowsAppInstallation(TOOL_VERSION, null, null, null));
    Git git = new Git(context);

    // act
    EditionAndVersion editionAndVersion = git.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("git");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of(TOOL_VERSION));
  }

  /**
   * Verifies that on Windows Git is not installed via a package manager but via the standard installer artifact, so the installer is launched with the expected
   * silent arguments (see {@link Git#getInstallerArguments()}).
   */
  @Test
  void testGetInstallerArgumentsOnWindows() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    Git git = new Git(context);

    //act
    List<String> gitArguments = git.getInstallerArguments();

    // assert
    assertThat(gitArguments).containsExactly("/VERYSILENT", "/NORESTART", "/NOCANCEL", "/SP-");
  }

  /**
   * Verifies that on macOS and Linux Git is installed via the package manager, so no installer arguments are produced.
   */
  @Test
  void testGetInstallerArgumentsEmptyOnMacAndLinux() {

    // arrange
    IdeTestContext macContext = newContext(PROJECT_BASIC);
    macContext.setSystemInfo(SystemInfoMock.MAC_X64);

    IdeTestContext linuxContext = newContext(PROJECT_BASIC);
    linuxContext.setSystemInfo(SystemInfoMock.LINUX_X64);

    //act
    List<String> macInstallerArguments = new Git(macContext).getInstallerArguments();
    List<String> linuxInstallerArguments = new Git(linuxContext).getInstallerArguments();

    //assert
    assertThat(macInstallerArguments).isEmpty();
    assertThat(linuxInstallerArguments).isEmpty();
  }
}
