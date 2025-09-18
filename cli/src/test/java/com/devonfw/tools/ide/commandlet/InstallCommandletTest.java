package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.repository.DefaultToolRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link InstallCommandlet}.
 */
@WireMockTest
public class InstallCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link InstallCommandlet} run, when Installed Version is null.
   *
   * @param wmRuntimeInfo wireMock server on a random port
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
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testInstallCommandletRunWithVersionAndVersionIdentifier(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    String version17 = "17.0.6";
    install.version.setValueAsString(version17, context);
    // act
    install.run();
    // assert
    assertTestInstall(context);
    assertThat(context).logAtSuccess().hasMessage("Successfully installed java in version " + version17);
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(version17);

    // now we install a different version

    // arrange
    String version21 = "21.0.8_9";
    install.version.setValueAsString(version21, context);
    // act
    install.run();
    // assert
    assertTestInstall(context);
    assertThat(context).logAtSuccess().hasMessage("Successfully installed java in version " + version21 + " replacing previous version " + version17);
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(version21);

    // now we install the initial version again that should just change the symlink

    // arrange
    install.version.setValueAsString(version17, context);
    // act
    install.run();
    // assert
    assertTestInstall(context);
    assertThat(context).logAtDebug().hasMessage("Version " + version17 + " of tool java is already installed at " + context.getSoftwareRepositoryPath().resolve(
        DefaultToolRepository.ID_DEFAULT).resolve("java").resolve("java").resolve(version17));
    assertThat(context).logAtSuccess().hasMessage("Successfully installed java in version " + version17 + " replacing previous version " + version21);
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(version17);
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
