package com.devonfw.tools.ide.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelperMock;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link GlobalToolCommandlet}.
 */
class GlobalToolCommandletTest extends AbstractIdeContextTest {

  private static final String DUMMY_BINARY = "dummy";

  private static final String TOOL_NAME = "docker";

  private static final String TOOL_VERSION = "1.21.0";

  /**
   * Tests that on macOS the installation path is resolved from the app bundle in the applications folder and its executable folder is registered.
   *
   * @param tempDir the temporary directory.
   * @throws IOException on test setup failure.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void testGetInstallationPathFindsMacApp(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Path applicationsPath = tempDir.resolve("Applications");
    Path appPath = applicationsPath.resolve("Dummy.app");
    Path appBinPath = appPath.resolve("Contents").resolve("MacOS");
    Path binary = appBinPath.resolve(DUMMY_BINARY);
    Files.createDirectories(appBinPath);
    Files.writeString(binary, "test");
    context.getFileAccess().makeExecutable(binary);
    GlobalToolCommandlet globalTool = new GlobalToolDummyCommandlet(context, applicationsPath);

    // act
    Path result = globalTool.getInstallationPath("default", VersionIdentifier.of("1.0"));

    // assert
    assertThat(result).isEqualTo(appPath);
    assertThat(context.getPath().getPath(DUMMY_BINARY)).isEqualTo(appBinPath);
  }

  /**
   * Tests that the applications folder is only searched on macOS and not on other operating systems.
   *
   * @param tempDir the temporary directory.
   * @throws IOException on test setup failure.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void testGetInstallationPathDoesNotSearchApplicationsOnLinux(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Path applicationsPath = tempDir.resolve("Applications");
    Path appBinPath = applicationsPath.resolve("Dummy.app").resolve("Contents").resolve("MacOS");
    Path binary = appBinPath.resolve(DUMMY_BINARY);
    Files.createDirectories(appBinPath);
    Files.writeString(binary, "test");
    context.getFileAccess().makeExecutable(binary);
    GlobalToolCommandlet globalTool = new GlobalToolDummyCommandlet(context, applicationsPath);

    // act & assert
    assertThat(globalTool.getInstallationPath("default", VersionIdentifier.of("1.0"))).isNull();
    assertThat(context.getPath().getPath(DUMMY_BINARY)).isNull();
  }

  /**
   * Tests that a macOS DMG is extracted and its application bundle is installed with a logged privileged move instead of executing the DMG.
   *
   * @param tempDir the temporary directory.
   * @throws IOException on test setup failure.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void testInstallMacDmgExtractsAndMovesApplication(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setIdeHome(tempDir);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    ProcessContext processContext = Mockito.mock(ProcessContext.class, Mockito.RETURNS_SELF);
    context.setProcessContext(processContext);
    context.getFileAccess().mkdirs(context.getTempPath());
    Path mountedApp = tempDir.resolve(IdeContext.FOLDER_UPDATES).resolve(IdeContext.FOLDER_VOLUME).resolve("Dummy.app");
    Path mountedBinary = mountedApp.resolve("Contents/MacOS").resolve(DUMMY_BINARY);
    Files.createDirectories(mountedBinary.getParent());
    Files.writeString(mountedBinary, "test");
    context.getFileAccess().makeExecutable(mountedBinary);
    Path applicationsPath = tempDir.resolve("Applications");
    Files.createDirectories(applicationsPath);
    GlobalToolCommandlet globalTool = new GlobalToolDummyCommandlet(context, applicationsPath);
    Path dmg = tempDir.resolve("Dummy.dmg");
    Path extractedApp = context.getTempPath().resolve(DUMMY_BINARY).resolve("Dummy.app");
    Path targetApp = applicationsPath.resolve("Dummy.app");

    // act
    globalTool.installMacDmg(dmg);

    // assert
    assertThat(context).logAtInteraction().hasMessageContaining("sudo /bin/mv " + extractedApp + " " + targetApp);
    Mockito.verify(processContext).executable("sudo");
    Mockito.verify(processContext, Mockito.never()).executable(dmg);
    assertThat(extractedApp.getParent()).doesNotExist();
  }

  private static class GlobalToolDummyCommandlet extends GlobalToolCommandlet {

    private final Path applicationsPath;

    GlobalToolDummyCommandlet(IdeContext context, Path applicationsPath) {

      super(context, DUMMY_BINARY, Set.of(Tag.TEST));
      this.applicationsPath = applicationsPath;
    }

    @Override
    protected String getBinaryName() {

      return DUMMY_BINARY;
    }

    @Override
    protected Path getMacApplicationsPath() {

      return this.applicationsPath;
    }
  }

  /**
   * Dummy {@link GlobalToolCommandlet} that simulates a background GUI installer (e.g. Rancher Desktop on Windows). Only {@code doInstall} is overridden so the
   * warning-check inside the real {@code install()} is exercised.
   */
  static class AsyncInstallerToolCommandlet extends GlobalToolCommandlet {

    AsyncInstallerToolCommandlet(IdeContext context) {

      super(context, TOOL_NAME, Set.of(Tag.DOCKER));
    }

    @Override
    protected void completeRequest(ToolInstallRequest request) {

      VersionIdentifier version = VersionIdentifier.of(TOOL_VERSION);
      ToolEdition edition = new ToolEdition(TOOL_NAME, "rancher");
      ToolEditionAndVersion requested = new ToolEditionAndVersion(edition, version);
      requested.setResolvedVersion(version);
      request.setRequested(requested);
    }

    @Override
    protected ToolInstallation doInstall(ToolInstallRequest request) {

      VersionIdentifier version = VersionIdentifier.of(TOOL_VERSION);
      return new ToolInstallation(null, null, null, version, true, true);
    }
  }

  /**
   * Verifies that when {@code doInstall} signals an asynchronous background installation, the real {@code install()} logs the warning and {@code runTool}
   * returns exit code 0 without trying to execute the not-yet-available binary.
   */
  @Test
  void testInstallLogsWarningAndRunToolAbortsWhenInstallationIsAsynchronous() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    AsyncInstallerToolCommandlet commandlet = new AsyncInstallerToolCommandlet(context);

    // act
    ProcessResult result = commandlet.runTool(List.of("ps"));

    // assert: runTool returns 0 without crashing with "command not found"
    assertThat(result.getExitCode()).isEqualTo(0);
    // assert: warning was emitted by install() covering both "ide install docker" and "ide docker ps" paths
    assertThat(context).logAtWarning().hasMessageContaining("is currently running in the background!");
    assertThat(context).logAtWarning()
        .hasMessageContaining("rerun your 'ide' command in a new terminal session after the installation has completed.");
  }

  /**
   * Verifies that calling {@code install()} directly (as done by {@code ide install docker}) also logs the background-installation warning.
   */
  @Test
  void testInstallDirectlyAlsoLogsWarningWhenInstallationIsAsynchronous() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    AsyncInstallerToolCommandlet commandlet = new AsyncInstallerToolCommandlet(context);

    // act
    ToolInstallation installation = commandlet.install();

    // assert: the async flag is set
    assertThat(installation.installedAsynchronously()).isTrue();
    // assert: warning was logged even without runTool being called
    assertThat(context).logAtWarning().hasMessageContaining("is currently running in the background!");
    assertThat(context).logAtWarning()
        .hasMessageContaining("rerun your 'ide' command in a new terminal session after the installation has completed.");
  }

  /**
   * Tests that the default Windows registry app name is the tool name.
   */
  @Test
  void testGetWindowsRegistryAppName() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    AsyncInstallerToolCommandlet commandlet = new AsyncInstallerToolCommandlet(context);

    // act + assert
    assertThat(commandlet.getWindowsRegistryAppName()).isEqualTo(TOOL_NAME);
  }

  /**
   * Tests that {@link GlobalToolCommandlet#getInstalledVersion()} reads the version from the Windows registry.
   */
  @Test
  void testGetInstalledVersionReadsVersionFromWindowsRegistry() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    helper.setAppInstallationFromRegistry(TOOL_NAME, new WindowsAppInstallation("2.3.4", null, null, null));
    AsyncInstallerToolCommandlet commandlet = new AsyncInstallerToolCommandlet(context);

    // act + assert
    assertThat(commandlet.getInstalledVersion()).isEqualTo(VersionIdentifier.of("2.3.4"));
  }

  /**
   * Tests that {@link GlobalToolCommandlet#getInstalledVersion()} returns {@code null} when no registry entry exists.
   */
  @Test
  void testGetInstalledVersionReturnsNullWhenRegistryEntryIsMissing() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    AsyncInstallerToolCommandlet commandlet = new AsyncInstallerToolCommandlet(context);

    // act + assert
    assertThat(commandlet.getInstalledVersion()).isNull();
  }

  /**
   * Dummy {@link GlobalToolCommandlet} that provides {@link NativePackage}s for testing uninstall via package manager on Linux.
   */
  static class PackageManagedToolCommandlet extends GlobalToolCommandlet {

    private static final String TOOL_NAME = "mytool";

    PackageManagedToolCommandlet(IdeContext context) {

      super(context, TOOL_NAME, Set.of(Tag.MISC));
    }

    @Override
    protected List<NativePackage> getNativePackages() {

      return List.of(
          new NativePackage(
              NativePackageManager.APT,
              List.of("mytool"),
              List.of(),
              List.of(),
              List.of("sudo rm -f /etc/apt/sources.list.d/mytool.list"))
      );
    }

    @Override
    protected String getBinaryName() {
      return TOOL_NAME;
    }
  }

  /**
   * Verifies that {@link GlobalToolCommandlet#getUninstallPackageManagerCommands()} correctly derives uninstall commands from
   * {@link GlobalToolCommandlet#getNativePackages()}.
   */
  @Test
  void testGetUninstallPackageManagerCommandsDerivesFromNativePackages() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    PackageManagedToolCommandlet commandlet = new PackageManagedToolCommandlet(context);

    // act
    List<PackageManagerCommand> uninstallCommands = commandlet.getUninstallPackageManagerCommands();

    // assert: exactly one command for APT
    assertThat(uninstallCommands).hasSize(1);
    PackageManagerCommand cmd = uninstallCommands.getFirst();
    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.APT);
    // The uninstall command includes the package removal and the cleanup command
    assertThat(cmd.commands()).containsExactly(
        "sudo apt -y autoremove --purge mytool",
        "sudo rm -f /etc/apt/sources.list.d/mytool.list");
  }
}
