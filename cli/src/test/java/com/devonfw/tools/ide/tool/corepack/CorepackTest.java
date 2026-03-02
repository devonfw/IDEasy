package com.devonfw.tools.ide.tool.corepack;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Corepack}.
 */
@WireMockTest
class CorepackTest extends AbstractIdeContextTest {

  private static final String PROJECT_COREPACK = "corepack";

  /**
   * Tests if the {@link Corepack} install works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testCorepackInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_COREPACK, wireMockRuntimeInfo);
    Corepack commandlet = new Corepack(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Corepack} uninstall works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testCorepackUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_COREPACK, wireMockRuntimeInfo);
    Corepack commandlet = new Corepack(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasNoMessageContaining("npm uninstall -g corepack");
    assertThat(context).logAtInfo().hasMessageContaining("IDEasy does not support uninstalling the tool corepack since this will break your installation.\n"
        + "If you really want to uninstall it, please uninstall its parent tool via:\n"
        + "ide uninstall node");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled corepack");
  }

  /**
   * Tests if {@link Corepack} run works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testCorepackRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_COREPACK, wireMockRuntimeInfo);
    Corepack commandlet = new Corepack(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("corepack --version");
    assertThat(context).logAtInfo().hasMessageContaining("corepack version: 0.34.0");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -gf corepack@0.34.0");
    assertThat(context).logAtSuccess().hasMessageContaining("Setting npm config prefix to: " + context.getSoftwarePath().resolve("node") + " was successful");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed corepack in version 0.34.0");
  }
}
