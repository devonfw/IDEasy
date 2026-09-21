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
import com.devonfw.tools.ide.tool.EditionAndVersion;
import com.devonfw.tools.ide.tool.NativePackage;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Docker}.
 */
class DockerTest extends AbstractIdeContextTest {

  private static final String APT_LIST_COMMAND = "apt list --installed | grep docker-desktop | awk '{print $2}'";

  private static final String PLUTIL_MAC_COMMAND = "plutil -extract CFBundleShortVersionString raw /Applications/Docker.app/Contents/Info.plist";

  private static final String RANCHER_PLUTIL_MAC_COMMAND =
      "plutil -extract CFBundleShortVersionString raw /Applications/Rancher Desktop.app/Contents/Info.plist";

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
    Mockito.when(processContext.runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", PLUTIL_MAC_COMMAND)).thenReturn("4.44.0");

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("docker");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("4.44.0"));
    Mockito.verify(processContext).runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", PLUTIL_MAC_COMMAND);
  }

  /**
   * Verifies that on macOS the lookup degrades gracefully (no exception) when the Docker Desktop app is not present: {@code plutil} exits
   * non-zero when the {@code .app} bundle is missing, so no edition resolves a version and the whole lookup returns {@code null} instead of
   * propagating the failure.
   */
  @Test
  void testDockerDesktopOnMacIsGracefulWhenAppMissing() {

    // arrange: plutil fails (non-zero exit) because /Applications/Docker.app is missing -> simulate the failed exit code
    ProcessContext processContext = Mockito.mock(ProcessContext.class);
    IdeTestContext context = newContext(processContext);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Docker docker = docker(context, "docker");
    Mockito.when(processContext.runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", PLUTIL_MAC_COMMAND))
        .thenThrow(new IllegalStateException("bash -lc plutil ... failed with exit code 1!"));

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert: the failed plutil is swallowed -> no edition resolves a version, so the lookup returns null (gracefully, no exception)
    assertThat(editionAndVersion).isNull();
    Mockito.verify(processContext).runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", PLUTIL_MAC_COMMAND);
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

    // arrange: Docker Desktop is not installed (no docker command, Docker.app missing so plutil fails with a non-zero exit code);
    // Rancher Desktop is installed
    ProcessContext processContext = Mockito.mock(ProcessContext.class);
    IdeTestContext context = newContext(processContext);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Docker docker = docker(context);
    Mockito.when(processContext.runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", PLUTIL_MAC_COMMAND))
        .thenThrow(new IllegalStateException("bash -lc plutil ... failed with exit code 1!"));
    Mockito.when(processContext.runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", RANCHER_PLUTIL_MAC_COMMAND)).thenReturn("1.13.0");

    // act
    EditionAndVersion editionAndVersion = docker.getInstalledEditionAndVersion();

    // assert: edition is the real "rancher" edition and the version comes from the Rancher Desktop.app bundle
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("rancher");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("1.13.0"));
    Mockito.verify(processContext).runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", RANCHER_PLUTIL_MAC_COMMAND);
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
