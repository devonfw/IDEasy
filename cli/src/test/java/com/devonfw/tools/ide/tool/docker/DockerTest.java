package com.devonfw.tools.ide.tool.docker;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelperMock;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.EditionAndVersion;
import com.devonfw.tools.ide.tool.NativePackage;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Docker}.
 */
class DockerTest extends AbstractIdeContextTest {

  private static final String APT_LIST_COMMAND = "apt list --installed | grep docker-desktop | awk '{print $2}'";

  private static final String PLUTIL_EXECUTABLE = "plutil";

  private static final String PLUTIL_ARGUMENT_1 = "-extract";

  private static final String PLUTIL_ARGUMENT_2 = "CFBundleShortVersionString";

  private static final String PLUTIL_ARGUMENT_3 = "raw";

  /** The full Docker Desktop {@code Info.plist} path passed to {@code plutil} as a single argument. */
  private static final String DOCKER_PLIST_PATH = "/Applications/Docker.app/Contents/Info.plist";

  /** The full Rancher Desktop {@code Info.plist} path (it contains a space) passed to {@code plutil} as a single argument. */
  private static final String RANCHER_PLIST_PATH = "/Applications/Rancher Desktop.app/Contents/Info.plist";

  /**
   * Creates a minimal {@link IdeTestContext} that returns a mocked {@link ProcessContext} from {@code createProcessContext()}
   * (which is what {@code newProcess()} delegates to), so no real process is started.
   *
   * @param processContext the {@link ProcessContext} to use.
   * @return the {@link IdeTestContext}.
   */
  private IdeTestContext newContext(ProcessContext processContext) {

    return new IdeTestContext(Path.of("/"), null) {
      @Override
      protected ProcessContext createProcessContext() {
        return processContext;
      }
    };
  }

  /**
   * A {@link Docker} that reports only the given commands as available.
   *
   * @param context the {@link IdeTestContext}.
   * @param availableCommands the command names that {@code isCommandAvailable} should report as available.
   * @return the {@link Docker}.
   */
  private Docker docker(IdeTestContext context, String... availableCommands) {

    return new Docker(context) {
      @Override
      protected boolean isCommandAvailable(String command) {
        return List.of(availableCommands).contains(command);
      }
    };
  }

  /**
   * Stubs the {@code plutil} lookup that {@link Docker#getMacAppVersion(String)} performs: it is invoked as a single
   * {@link ProcessContext#runAndGetSingleOutput} call with the executable and the four plutil arguments (the {@code Info.plist} path is one
   * argument), so the mock's {@code errorHandling(NONE)} must return the mock itself for the chained call to work.
   *
   * @param processContext the mocked {@link ProcessContext}.
   * @param plistPath the full {@code Info.plist} path to stub.
   * @param output the version string {@code plutil} should print, or {@code null} to simulate a failed/absent lookup.
   */
  private void stubPlutil(ProcessContext processContext, String plistPath, String output) {

    Mockito.when(processContext.errorHandling(ProcessErrorHandling.NONE)).thenReturn(processContext);
    Mockito.when(processContext.runAndGetSingleOutput(IdeLogLevel.WARNING, PLUTIL_EXECUTABLE, PLUTIL_ARGUMENT_1, PLUTIL_ARGUMENT_2,
        PLUTIL_ARGUMENT_3, plistPath)).thenReturn(output);
  }

  /**
   * Verifies that the edition of a (non-rancher) Docker Desktop installation is reported as {@code "docker"} — the consistent
   * name used by {@link Docker#getWindowsRegistryAppNames()} — and not the previously hard-coded, non-existent edition
   * {@code "desktop"}. The version is resolved from the per-OS source (here the Linux {@code apt list} output).
   */
  @Test
  void testDockerDesktopEditionIsDockerOnLinux() {

    // arrange
    ProcessContext processContext = Mockito.mock(ProcessContext.class);
    IdeTestContext context = newContext(processContext);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Docker docker = docker(context, "docker");
    Mockito.when(processContext.runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", APT_LIST_COMMAND)).thenReturn("20.10.5-ubuntu-focal");

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("docker");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("20.10.5"));
    Mockito.verify(processContext).runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", APT_LIST_COMMAND);
  }

  /**
   * Verifies that on macOS the version of a Docker Desktop installation is resolved from the {@code Docker.app} bundle
   * (via {@code plutil}), so the returned version is not {@code null}, while the edition stays the consistent {@code "docker"}.
   */
  @Test
  void testDockerDesktopEditionAndVersionOnMac() {

    // arrange
    ProcessContext processContext = Mockito.mock(ProcessContext.class);
    IdeTestContext context = newContext(processContext);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Docker docker = docker(context, "docker");
    stubPlutil(processContext, DOCKER_PLIST_PATH, "4.44.0");

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert: plutil is called directly (not via `bash -lc`) and the full Docker.app plist path is passed as one argument
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("docker");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("4.44.0"));
    Mockito.verify(processContext).runAndGetSingleOutput(IdeLogLevel.WARNING, PLUTIL_EXECUTABLE, PLUTIL_ARGUMENT_1, PLUTIL_ARGUMENT_2,
        PLUTIL_ARGUMENT_3, DOCKER_PLIST_PATH);
  }

  /**
   * Verifies that on macOS the lookup degrades gracefully (no exception) when the Docker Desktop app is not present: {@code plutil} exits
   * non-zero when the {@code .app} bundle is missing, so no edition resolves a version and the whole lookup returns {@code null} instead of
   * propagating the failure.
   */
  @Test
  void testDockerDesktopOnMacIsGracefulWhenAppMissing() {

    // arrange: plutil fails (non-zero exit) because /Applications/Docker.app is missing. With errorHandling(NONE) run() no longer throws, so
    // the failed plutil surfaces as a null single output instead of an exception.
    ProcessContext processContext = Mockito.mock(ProcessContext.class);
    IdeTestContext context = newContext(processContext);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Docker docker = docker(context, "docker");
    stubPlutil(processContext, DOCKER_PLIST_PATH, null);

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert: the failed plutil is swallowed -> no edition resolves a version, so the lookup returns null (gracefully, no exception)
    assertThat(editionAndVersion).isNull();
    Mockito.verify(processContext).runAndGetSingleOutput(IdeLogLevel.WARNING, PLUTIL_EXECUTABLE, PLUTIL_ARGUMENT_1, PLUTIL_ARGUMENT_2,
        PLUTIL_ARGUMENT_3, DOCKER_PLIST_PATH);
  }

  /**
   * Verifies that on Linux a Rancher Desktop installation is resolved to the {@code "rancher"} edition and its version is
   * read from the native package manager (the {@code rancher-desktop} package), not from the Docker Desktop
   * {@code apt list} source used for the {@code docker} edition. The Docker Desktop source is not consulted at all.
   */
  @Test
  void testRancherDesktopEditionAndVersionOnLinux() {

    // arrange: mock the protected native-package-manager helpers so no real package manager or filesystem access happens
    ProcessContext processContext = Mockito.mock(ProcessContext.class);
    IdeTestContext context = newContext(processContext);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Docker docker = new Docker(context) {
      @Override
      protected boolean isPackageManagerAvailable(NativePackageManager packageManager) {
        return packageManager == NativePackageManager.APT;
      }

      @Override
      protected String queryNativePackageVersion(NativePackage nativePackage) {
        return "1.13.0";
      }
    };

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert: edition is the real "rancher" edition (not a placeholder) and the version comes from the native package manager
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("rancher");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("1.13.0"));
  }

  /**
   * Verifies that on macOS a Rancher Desktop installation is resolved to the {@code "rancher"} edition and its version is read from the
   * {@code Rancher Desktop.app} bundle (via {@code plutil}) — the same app-bundle concept the {@code docker} edition uses on macOS. Docker Desktop
   * is absent (no {@code docker} command, empty {@code Docker.app} output), so the probe order ({@code docker} first) must fall through to
   * {@code rancher}. This closes the gap where macOS previously returned nothing for Rancher.
   */
  @Test
  void testRancherDesktopEditionAndVersionOnMac() {

    // arrange: Docker Desktop is not installed (no docker command, Docker.app missing so the plutil lookup fails with a non-zero exit
    // code -> null output); Rancher Desktop is installed. The failed docker lookup must not stop the probe: it must still fall through to
    // rancher.
    ProcessContext processContext = Mockito.mock(ProcessContext.class);
    IdeTestContext context = newContext(processContext);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Docker docker = docker(context);
    stubPlutil(processContext, DOCKER_PLIST_PATH, null);
    stubPlutil(processContext, RANCHER_PLIST_PATH, "1.13.0");

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert: edition is the real "rancher" edition and the version comes from the Rancher Desktop.app bundle (single-arg plist path)
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("rancher");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("1.13.0"));
    Mockito.verify(processContext).runAndGetSingleOutput(IdeLogLevel.WARNING, PLUTIL_EXECUTABLE, PLUTIL_ARGUMENT_1, PLUTIL_ARGUMENT_2,
        PLUTIL_ARGUMENT_3, RANCHER_PLIST_PATH);
  }

  /**
   * Verifies that on Windows the version of a Docker Desktop installation is read from the Windows registry (via
   * {@code super}), and the edition is the consistent {@code "docker"} (not the previously hard-coded {@code "desktop"}).
   */
  @Test
  void testDockerDesktopEditionAndVersionOnWindows() {

    // arrange
    IdeTestContext context = newContext(Mockito.mock(ProcessContext.class));
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    helper.setAppInstallationFromRegistry("Docker Desktop", new WindowsAppInstallation("4.44.0", null, null, null));
    Docker docker = docker(context, "docker");

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("docker");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("4.44.0"));
  }

  /**
   * Verifies that when both Docker Desktop and Rancher Desktop are installed on Windows (both registry entries present) the
   * {@code docker} edition is resolved, because it is probed first (see {@link Docker#getEditionNames()}).
   */
  @Test
  void testBothDockerDesktopAndRancherDesktopInstalledOnWindowsResolvesDocker() {

    // arrange: both apps present in the (mocked) registry
    IdeTestContext context = newContext(Mockito.mock(ProcessContext.class));
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    helper.setAppInstallationFromRegistry("Docker Desktop", new WindowsAppInstallation("4.44.0", null, null, null));
    helper.setAppInstallationFromRegistry("Rancher Desktop", new WindowsAppInstallation("1.24.0", null, null, null));
    Docker docker = docker(context, "docker");

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert: the docker edition wins because it is probed before rancher
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("docker");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("4.44.0"));
  }

  /**
   * Verifies that on Windows with only Rancher Desktop installed (no Docker Desktop registry entry) the resolution falls through to
   * the {@code rancher} edition, with the version read from the {@code Rancher Desktop} registry entry.
   */
  @Test
  void testRancherDesktopOnlyOnWindowsResolvesRancher() {

    // arrange: only Rancher Desktop present in the (mocked) registry
    IdeTestContext context = newContext(Mockito.mock(ProcessContext.class));
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    helper.setAppInstallationFromRegistry("Rancher Desktop", new WindowsAppInstallation("1.24.0", null, null, null));
    Docker docker = docker(context);

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert: the rancher edition with the version of the Rancher Desktop registry entry
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("rancher");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("1.24.0"));
  }

  /**
   * Sanity check that the Windows registry edition names are keyed by the same consistent edition names used by
   * {@link Docker#computeInstalledEditionAndVersion()} (the non-rancher edition is {@code "docker"}, not {@code "desktop"}).
   */
  @Test
  void testWindowsRegistryAppNamesUseConsistentEditionNames() {

    // arrange
    IdeTestContext context = newContext(Mockito.mock(ProcessContext.class));
    Docker docker = new Docker(context);

    // act
    Map<String, String> appNames = docker.getWindowsRegistryAppNames();

    // assert
    assertThat(appNames).containsEntry("docker", "Docker Desktop").containsEntry("rancher", "Rancher Desktop");
  }

}
