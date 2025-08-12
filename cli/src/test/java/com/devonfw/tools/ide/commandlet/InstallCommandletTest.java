package com.devonfw.tools.ide.commandlet;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link InstallCommandlet}.
 */
@WireMockTest
public class InstallCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link InstallCommandlet} run, when Installed Version is null.
   */
  @Test
  public void testInstallCommandletRunWithVersion(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    // act
    install.run();
    // assert
    assertTestInstall(context);
  }

  /**
   * Test of {@link InstallCommandlet} run, when Installed Version is set.
   */
  @Test
  public void testInstallCommandletRunWithVersionAndVersionIdentifier(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    install.version.setValueAsString("17.0.6", context);

    // act
    install.run();
    // assert
    assertTestInstall(context);
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent("17.0.6");
  }

  private void assertTestInstall(IdeContext context) {

    assertThat(context.getSoftwarePath().resolve("java")).exists();
    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("java/bin/java.cmd")).exists();
    } else if (context.getSystemInfo().isLinux() || context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    }
  }
}
