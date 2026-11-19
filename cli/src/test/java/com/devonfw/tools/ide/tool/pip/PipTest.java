package com.devonfw.tools.ide.tool.pip;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
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
   * A version query of pip must be handled by pip itself, even if the package manager edition is {@code uv}: the {@code pip} sub-command of {@code uv} does not
   * support a {@code --version} flag (see
   * <a href="https://github.com/devonfw/IDEasy/issues/2561">#2561</a>), so {@code ide pip --version} must not be rewritten to {@code uv pip --version}.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testPipVersionQueryWithUv(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    // the pip fixture project ships with PIP_EDITION=pip - switch to the default uv edition
    context.getVariables().getByType(EnvironmentVariablesType.SETTINGS).set("PIP_EDITION", "uv");
    Pip commandlet = new Pip(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("pip --version");
    assertThat(context).logAtInfo().hasNoMessageContaining("uv pip --version");
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
    assertThat(versions).containsExactly(VersionIdentifier.ofRequired("24.2"), VersionIdentifier.ofRequired("24.1"), VersionIdentifier.ofRequired("24.0"));
  }

  private void checkInstallation(IdeTestContext context) {

    // Pip is installed via uv pip install pip==<version>
    assertThat(context).logAtInfo().hasMessageContaining("uv pip install pip==");

    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed pip");
  }
}
