package com.devonfw.tools.ide.tool;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelperMock;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link GlobalToolCommandlet}.
 */
class GlobalToolCommandletTest extends AbstractIdeContextTest {

  private static final String TOOL_NAME = "docker";

  private static final String TOOL_VERSION = "1.21.0";

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
