package com.devonfw.tools.ide.tool;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link GlobalToolCommandlet}.
 */
class GlobalToolCommandletTest extends AbstractIdeContextTest {

  private static final String TOOL_NAME = "docker";

  private static final String TOOL_VERSION = "1.21.0";

  /**
   * Dummy {@link GlobalToolCommandlet} that simulates a background GUI installer.
   */
  static class AsyncInstallerToolCommandlet extends GlobalToolCommandlet {

    AsyncInstallerToolCommandlet(IdeContext context) {

      super(context, TOOL_NAME, Set.of(Tag.DOCKER));
    }

    @Override
    public ToolInstallation install(ToolInstallRequest request) {

      VersionIdentifier version = VersionIdentifier.of(TOOL_VERSION);
      ToolEdition edition = new ToolEdition(TOOL_NAME, "rancher");
      ToolEditionAndVersion requested = new ToolEditionAndVersion(edition, version);
      requested.setResolvedVersion(version);
      request.setRequested(requested);
      return new ToolInstallation(null, null, null, version, true, true);
    }

    @Override
    protected ToolInstallation doInstall(ToolInstallRequest request) {

      throw new UnsupportedOperationException("should not be called in this test");
    }
  }

  /**
   * Verifies that when a global tool installer runs in the background (asynchronously), {@link ToolCommandlet#runTool} logs a warning mentioning the tool
   * name and version, and returns exit code 0 without trying to execute the not-yet-available binary.
   */
  @Test
  void testRunToolAbortsWithWarningWhenInstallationIsAsynchronous() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    AsyncInstallerToolCommandlet commandlet = new AsyncInstallerToolCommandlet(context);

    // act
    ProcessResult result = commandlet.runTool(List.of("ps"));

    // assert
    assertThat(result.getExitCode()).isEqualTo(0);
    assertThat(context).logAtWarning().hasMessageContaining(TOOL_NAME);
    assertThat(context).logAtWarning().hasMessageContaining(TOOL_VERSION);
    assertThat(context).logAtWarning().hasMessageContaining("is currently running in the background!");
    assertThat(context).logAtWarning()
        .hasMessageContaining("rerun your 'ide' command in a new terminal session after the installation has completed.");
  }
}
