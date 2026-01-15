package com.devonfw.tools.ide.tool.pip;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Pip}.
 */
@WireMockTest
class PipTest extends AbstractIdeContextTest {

  private static final String PROJECT_PIP = "pip";

  /**
   * Tests that the {@link Pip} commandlet can be installed via uv pip install.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testPipInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Pip commandlet = new Pip(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests that the {@link Pip} commandlet run works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testPipRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Pip commandlet = new Pip(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("pip --version");
  }

  /**
   * Test {@link Pip#getVersions()} works correctly via {@link PipRepository}.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testPipVersions(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Pip commandlet = new Pip(context);

    // act
    List<VersionIdentifier> versions = commandlet.getVersions();

    // assert
    assertThat(versions).containsExactly(VersionIdentifier.of("24.2"), VersionIdentifier.of("24.1"), VersionIdentifier.of("24.0"));
  }

  private void checkInstallation(IdeTestContext context) {

    // Pip is installed via uv pip install pip==<version>
    assertThat(context).logAtInfo().hasMessageContaining("uv pip install pip==");

    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed pip");
  }
}
